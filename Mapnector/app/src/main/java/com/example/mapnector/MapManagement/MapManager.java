package com.example.mapnector.MapManagement;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import androidx.core.app.ActivityCompat;

import com.example.mapnector.DBStorage.GroupUser;
import com.example.mapnector.FirebaseCommunication.FirebaseHelper;
import com.example.mapnector.FirebaseCommunication.FirebaseLocationSystem;
import com.example.mapnector.GroupManagement.LVHelper;
import com.example.mapnector.LVAdapters.AdapterManager;
import com.example.mapnector.UserInterface.UIHelper;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * the job of the class is to handle all the logic required for the location system - to share my location, see others locations, check permissions, update location, clear
 * my data when app stopped, manage both threads etc
 */
public class MapManager implements View.OnClickListener {

    /**
     * a constant; if latitude or longitude equals to 300(not possible value in real life) so we know users location not available
     */
    public static final int NULL_LOC = 300; //300 not possible value so suppose its null
    /**
     * How many time in milliseconds to sleep in threads; i chose 5000 milliseconds but can be easily updated using this variable
     */
    private final int TICK_TIME_MILLIS = 5000;

    /**
     * reference to google map we got asynchronously in MapsActivity
     */
    private final GoogleMap googleMap;
    /**
     * ref to FBhelper created in LVhelper
     */
    private final FirebaseHelper FBhelper;

    /**
     * ref to LocationManger we retrieved from MapsActivtiy
     */
    private final LocationManager locationManager;

    /**
     * Thread that updates mine location in the firebase; if don't have permission or checkbox to allow that is off thread is disabled
     */
    private LocationThread lt;
    /**
     * Thread that retrieves other group users location in the firebase; if im in groups view(of the left listView) thread off, if clicked on some group thread activated
     */
    private LocationGetterThread lgt;

    /**
     * used in locationThread; one of the 2 variables that if one false LocationThread interrupted; if true we send location to firebase if false we disable the thread that does
     * that and clean our location data in firebase to latitude,longitude = 300,300
     */
    private boolean keepThreadCheckbox;
    /**
     * used in locationThread; one of the 2 variables that if one false LocationThread interrupted; in general true, if false means onPause called, and we dont want to share location
     * when app in background
     */
    public boolean keepThreadPause;

    /**
     * ref to MapsActivtiy
     */
    private final Activity activity;
    /**
     * because when we get others locations in LocationGetterThread we cant update ui, we use handler
     */
    private final Handler handler;

    /**
     * ref to UIhelper created in MapsActivity
     */
    private final UIHelper uiHelper;

    /**
     * represents mine current location latitude
     */
    public static double mLatitude = NULL_LOC;
    /**
     * represents mine current location longitude
     */
    public static double mLongitude = NULL_LOC;

    /**
     * used to store other users location by key - DoubleDoubleName object(when key of the map is the uid of the user we store his location)
     */
    private final HashMap<String, FirebaseLocationSystem.DoubleDoubleName> map; //key-users uid

    /**
     * here we initialize variables, if we have location permission enable my location in map
     * @param activity MapsActivtiy
     * @param googleMap the map we got async in MapsActivtiy
     * @param FBhelper ref to FirebaseHelper
     * @param locationManager ref to LocationManger we got in MapsActivtiy
     * @param uiHelper ref to UIHelper
     */
    @SuppressLint("MissingPermission")
    public MapManager(Activity activity, GoogleMap googleMap, FirebaseHelper FBhelper,
                      LocationManager locationManager, UIHelper uiHelper) {
        this.googleMap = googleMap;
        this.FBhelper = FBhelper;
        this.locationManager = locationManager;
        this.activity = activity;
        this.uiHelper = uiHelper;

        this.uiHelper.chkSendLocation.setOnClickListener(this);
        this.uiHelper.chkAccessLocation.setOnClickListener(this);

        this.keepThreadPause = true;
        this.keepThreadCheckbox = false;

        if(ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            googleMap.setMyLocationEnabled(true);

        this.map = new HashMap<String, FirebaseLocationSystem.DoubleDoubleName>();

        handler = new android.os.Handler(msg -> { //msg.what == 11 means update with map, 22 means just clear map
            googleMap.clear();
            if(msg.what == 11) {
                this.drawMapData();
            }
            return true;
        });

        this.considerStartLocationGetterThread();
    }

    /**
     * move camera to the place the GroupUser located; if place unknown do nothing
     * @param gu the groupUser to move the camera to
     */
    public void focusCamera(GroupUser gu) {
        FirebaseLocationSystem.DoubleDoubleName ddn = map.get(gu.uid);
        if(ddn != null && ddn.latitude != NULL_LOC && ddn.longitude != NULL_LOC) {
            CameraUpdate location = CameraUpdateFactory.newLatLngZoom(new LatLng(ddn.latitude, ddn.longitude), 15);
            googleMap.animateCamera(location);
        }
    }

    /**
     * we get there when UpdateMap in menu in MapsActivity is clicked - move to the next map type in the hierarchy
     */
    public void updateMapType() {
        int type = GoogleMap.MAP_TYPE_NORMAL;
        switch(googleMap.getMapType()) {
            case GoogleMap.MAP_TYPE_NORMAL:
                type = GoogleMap.MAP_TYPE_HYBRID;
                break;
            case GoogleMap.MAP_TYPE_HYBRID:
                type = GoogleMap.MAP_TYPE_SATELLITE;
                break;
            case GoogleMap.MAP_TYPE_SATELLITE:
                type = GoogleMap.MAP_TYPE_TERRAIN;
                break;
            case GoogleMap.MAP_TYPE_TERRAIN:
                type = GoogleMap.MAP_TYPE_NORMAL;
                break;
        }
        googleMap.setMapType(type);
    }

    /**
     * handler calls this function(and MapsActivity in some specific cases to not wait 5 seconds before showing data); we iterate the hashMap the FirebaseLocationSystem
     * fills in and add markers to those locations in the googleMap(without marker to our own location)
     */
    public void drawMapData() {
        try {
            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                if(!pair.getKey().toString().equals(FirebaseHelper.current.uid)) {
                    FirebaseLocationSystem.DoubleDoubleName p = (FirebaseLocationSystem.DoubleDoubleName)pair.getValue();
                    if(p.longitude == NULL_LOC || p.latitude == NULL_LOC)
                        continue;
                    LatLng pos = new LatLng(p.latitude, p.longitude);
                    googleMap.addMarker(new MarkerOptions().position(pos).title(p.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(p.color)));
                }
            }
        }
        catch(ConcurrentModificationException ignored) {} //small chance, but can happen, to avoid crash
    }

    /**
     * checks for location permission, if already granted returns true if not asks for it and returns false
     * @return true if location permission already granted before and false otherwise
     */
    public boolean checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            return false;
        }
        return true;
    }

    /**
     * this function called if onResume of MapsActivity called; checks whether threads are running, whether to stop them and that stuff
     */
    public void activityResumed() {
        this.keepThreadPause = true;
        if(keepThreadCheckbox) { //if checked means not unchecked means got permission. btw means lt != null
            if (!lt.running) {
                lt = new LocationThread();
                lt.start();
                googleMap.clear();
            }
        }
    }

    /**
     * called when checkBox to send location to firebase clicked; if enabled remains/starts locationThread if false makes keepThreadCheckbox
     * false what will shutdown the thread in 5 seconds(or less)
     */
    public void chkBoxSendLocClick() {
        if (uiHelper.chkSendLocation.isChecked()) {
            if (this.checkAndRequestLocationPermission()) {
                keepThreadCheckbox = true;
                if (lt == null || !lt.running) {
                    lt = new LocationThread();
                    lt.start();
                }
            } else
                uiHelper.chkSendLocation.setChecked(false);
        } else {
            keepThreadCheckbox = false;
        }
    }

    /**
     * handles clicks on checkboxes - uiHelper.chkSendLocation(if to send location in general), uiHelper.chkAccessLocation(if allow current group to access location);
     * @param v the clicked view
     */
    @Override
    public void onClick(View v) {
        if (v == uiHelper.chkSendLocation) {
            chkBoxSendLocClick();
        }
        else if(v == uiHelper.chkAccessLocation) {
            FBhelper.fls.DB_groupMineLocationAccess(uiHelper.chkAccessLocation.isChecked());
        }
    }

    /**
     * if lgt thread isn't working - create a new one and start it, but if its already running do nothing
     */
    public void considerStartLocationGetterThread() {
        if(lgt == null || !lgt.running) {
            lgt = new LocationGetterThread();
            lgt.start();
        }
    }

    /**
     * clear google map from markers
     */
    public void clearMap() {this.googleMap.clear();}

    /**
     * set my location data in firebase as 300 for both variables; requests firebase to update it
     */
    public void resetLocation() {
        mLatitude = NULL_LOC;
        mLongitude = NULL_LOC;
        FBhelper.fls.DB_changeMineLocation();
    }


    //mine threads

    /**
     * Thread that's aim is to get other groupUsers locations from firebase
     */
    public class LocationGetterThread extends Thread {

        /**
         * indicates whether the thread running or not
         */
        private boolean running;

        public LocationGetterThread() {
            this.running = false;
        }


        /**
         * while the application MapsActivity not paused, retrieve other groupUsers location from firebase and update 'map'; then request via handler
         * the ui thread to update markers
         */
        @Override
        public void run() {
            this.running = true;
            while(keepThreadPause) {
                if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_USERS && AdapterManager.haveLastSelectedGroup) {
                    FBhelper.fls.DB_updateUsersLocations(map);
                    Message msg = new Message();
                    msg.what = 11;
                    handler.sendMessage(msg);
                }
                else {
                    Message msg = new Message();
                    msg.what = 22;
                    handler.sendMessage(msg);
                }
                try {
                    Thread.sleep(TICK_TIME_MILLIS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            this.running = false;
        }
    }


    /**
     * Thread that's aim is to update my location data in firebase
     */
    public class LocationThread extends Thread {
        /**
         * indicates whether the thread running or not
         */
        private boolean running;

        public LocationThread() {
            this.running = true;
        }

        /**
         * while the application not paused and we allow to send locationupdate my location in database every 5 seconds;
         * if we dont ahve permission
         */
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            while (keepThreadCheckbox && keepThreadPause) {
                Criteria criteria = new Criteria();
                String provider = locationManager.getBestProvider(criteria, false);
                @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(provider);
                if(location != null) {
                    mLatitude = location.getLatitude();
                    mLongitude = location.getLongitude();
                    FBhelper.fls.DB_changeMineLocation();
                }

                try {
                    Thread.sleep(TICK_TIME_MILLIS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            resetLocation();
            this.running = false;
        }
    }

    public GoogleMap getGoogleMap() {return this.googleMap;}

    public HashMap<String, FirebaseLocationSystem.DoubleDoubleName> getUsersLocationsMap() {return this.map;}

}
