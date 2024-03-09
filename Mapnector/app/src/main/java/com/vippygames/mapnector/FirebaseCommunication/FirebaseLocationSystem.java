package com.vippygames.mapnector.FirebaseCommunication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vippygames.mapnector.DBStorage.Group;
import com.vippygames.mapnector.DBStorage.GroupUser;
import com.vippygames.mapnector.LVAdapters.AdapterManager;
import com.vippygames.mapnector.MapManagement.MapManager;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Communicates with firebase in all things that are associated with locations of users(retrieve others locations, update mine);
 * One of the 3 FirebaseCommunication classes - manages work that has to do with the location system
 */
public class FirebaseLocationSystem {

    /**
     * ref to ADmanager created in LVhelper
     */
    private final AdapterManager ADmanager;
    /**
     * list of groups that i allowed to them to access my location
     */
    private final ArrayList<Group> groupsAccessMyLocation;

    /**
     * small struct(class) that holds data we want to store about some user for future use; the data is the latitude,longitude, groupUser's name and color; explanation why color
     * is float view in color variable docs of the struct
     */
    public static class DoubleDoubleName {
        public double latitude;
        public double longitude;
        public String name;
        /**
         * there is a certain number of marker colors google map provides as; we access them via BitmapDescriptorFactory.HUE_BLUE, BitmapDescriptorFactory.HUE_RED and so on;
         * in their core those values are floats, so to save that data i made color float
         */
        public float color;

        public DoubleDoubleName(double latitude, double longitude, String name) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.name = name;
        }
    }

    /**
     * really nothing special, 2 rows that initialize the 2 variables
     * @param ADmanager reference to ADmanager created in LVHelper
     */
    public FirebaseLocationSystem(AdapterManager ADmanager) {
        this.ADmanager = ADmanager;
        this.groupsAccessMyLocation = new ArrayList<Group>();
    }


    /**
     * to make everything look better i decided to give every groupUser we can see in google map his own marker color; that's the job of this is function; again, why float
     * view DoublDoubleName.color description
     * @return float that indicates google map marker color(BitmapDescriptorFactory.something)
     */
    public float getRandomMarkerColor() {
        int num = (int)(Math.random() * 10);
        switch(num) {
            case 0:
                return BitmapDescriptorFactory.HUE_AZURE;
            case 1:
                return BitmapDescriptorFactory.HUE_BLUE;
            case 2:
                return BitmapDescriptorFactory.HUE_CYAN;
            case 3:
                return BitmapDescriptorFactory.HUE_GREEN;
            case 4:
                return BitmapDescriptorFactory.HUE_MAGENTA;
            case 5:
                return BitmapDescriptorFactory.HUE_ORANGE;
            case 6:
                return BitmapDescriptorFactory.HUE_RED;
            case 7:
                return BitmapDescriptorFactory.HUE_ROSE;
            case 8:
                return BitmapDescriptorFactory.HUE_VIOLET;
            case 9:
                return BitmapDescriptorFactory.HUE_YELLOW;
        }
        return BitmapDescriptorFactory.HUE_RED;
    }


    /**
     * we pass the HashMap from 'MapManager.class' and fill it there; we retrieve new users locations (that allow us access it) and create objects of DoublDobuleName to store
     * and show info later; if we don't have color for marker of that user generate random color(call getRandomMarkerColor)
     * @param map the hashMap to store the data for later use
     */
    public void DB_updateUsersLocations(HashMap<String, DoubleDoubleName> map) {
        for(GroupUser gr : ADmanager.lastSelectedGroup.groupUsers) {
            if(!gr.allowAccessLoc) {
                map.remove(gr.uid);
                continue;
            }

            FirebaseHelper.refUsers.orderByChild("uid").equalTo(gr.uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for(DataSnapshot sp : snapshot.getChildren()) {
                        DoubleDoubleName loc = new DoubleDoubleName(sp.child("latitude").getValue(double.class),
                                sp.child("longitude").getValue(double.class), sp.child("name").getValue(String.class));

                        String uid = sp.child("uid").getValue(String.class);
                        if(!map.containsKey(uid)) {
                            loc.color = getRandomMarkerColor();
                        }
                        else
                            loc.color = map.get(uid).color;
                        map.put(uid, loc);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    /**
     * sets data about allowing/not allowing accessing my location to current group(ADmanager.lastSelectedGroup) dependent on parameter
     * @param allowAccess true if we want to allow this group access my location, false otherwise
     */
    public void DB_groupMineLocationAccess(boolean allowAccess) {
        FirebaseHelper.refGroups.child(ADmanager.lastSelectedGroup.key).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Group g = currentData.getValue(Group.class);
                GroupUser me = new GroupUser(FirebaseHelper.current.uid, "idk", "none", allowAccess);
                GroupUser newMe = g.groupUsers.get(g.groupUsers.indexOf(me));
                newMe.allowAccessLoc = allowAccess;
                int idx = g.groupUsers.indexOf(newMe);
                g.groupUsers.remove(newMe);
                g.groupUsers.add(idx, newMe);
                currentData.setValue(g);
                if(allowAccess)
                    groupsAccessMyLocation.add(ADmanager.lastSelectedGroup);
                else
                    groupsAccessMyLocation.remove(ADmanager.lastSelectedGroup);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

            }
        });
    }

    /**
     * iterate through 'groupsAccessMyLocation' and put false in 'allowAccessLoc' in firebase; none can view my location after this function
     */
    public void DB_clearAllGroupsWithMineLocationAccess() {
        for(Group g : groupsAccessMyLocation) {
            FirebaseHelper.refGroups.child(g.key).runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Group gr = currentData.getValue(Group.class);
                    GroupUser me = new GroupUser(FirebaseHelper.current.uid, "idk", "none", false);
                    GroupUser newMe = g.groupUsers.get(g.groupUsers.indexOf(me));
                    newMe.allowAccessLoc = false;
                    int idx = g.groupUsers.indexOf(newMe);
                    g.groupUsers.remove(newMe);
                    g.groupUsers.add(idx, newMe);
                    currentData.setValue(gr);
                    groupsAccessMyLocation.remove(gr);
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

                }
            });
        }
    }


    /**
     * set new latitude and longitude values in my location in firebase; values taken from public static variables of MapManager.class
     */
    public void DB_changeMineLocation() {
        FirebaseHelper.refUsers.child(FirebaseHelper.current.uid).child("latitude").setValue(MapManager.mLatitude);
        FirebaseHelper.refUsers.child(FirebaseHelper.current.uid).child("longitude").setValue(MapManager.mLongitude);
        FirebaseHelper.current.longitude = MapManager.mLongitude;
        FirebaseHelper.current.latitude = MapManager.mLatitude;
    }

    /**
     * true if current group(ADmanager.lastSelectedGroup) can access my location, false otherwise
     * @return true if current group(ADmanager.lastSelectedGroup) can access my location, false otherwise
     */
    public boolean canGroupAccessMineLocation() {
        return this.groupsAccessMyLocation.contains(ADmanager.lastSelectedGroup);
    }

}
