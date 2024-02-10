package com.vippygames.mapnector;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.vippygames.mapnector.ChatSystem.ChatActivity;
import com.vippygames.mapnector.DBStorage.Group;
import com.vippygames.mapnector.DBStorage.GroupUser;
import com.vippygames.mapnector.FirebaseCommunication.FirebaseGroupSystem;
import com.vippygames.mapnector.FirebaseCommunication.FirebaseHelper;
import com.vippygames.mapnector.GroupManagement.LVHelper;
import com.vippygames.mapnector.LVAdapters.AdapterManager;
import com.vippygames.mapnector.LVAdapters.GroupAdapter;
import com.vippygames.mapnector.LoginSystem.LoginActivity;
import com.vippygames.mapnector.MapManagement.MapManager;
import com.vippygames.mapnector.UserInterface.UIHelper;

/** The main activity. here the user can view/join/leave/manage his groups or create new ones, he can use the google
 * map features, and on the right he has a sliding pane that is his own mailbox */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    /** manages stuff of the left listView - divide and conqueror stuff */
    private LVHelper lvHelper;
    /**holds almost all views, buttons, editText etc., to make them accessible from anywhere*/
    private UIHelper uiHelper;
    /** class which's aim is to rule the map system(get location, share my location etc.)*/
    private MapManager mapManager;

    /** dynamically created dialog to allow user to change he's name  */
    private Dialog changeNameDialog;
    /** reference to the editText in the dynamically created dialog */
    private EditText edtChangeName;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    private void handleAbout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About");
        builder.setMessage("App version: " + BuildConfig.VERSION_NAME);
        builder.setPositiveButton("Get Help", (dialog, which) -> {
            ExternalAppUtils externalAppUtils = new ExternalAppUtils(this);
            externalAppUtils.tryOpenUri("mailto:vippygames@gmail.com", "No email app found");
        });
        builder.setNeutralButton("Cancel", null);
        AlertDialog dialog = builder.create();

        dialog.show();
    }

    /** handles on menu click: action_changemap, action_logout, action_namechange. For changemap - calls mapManager to change map type. If logout - returns back to LoginActivity and removed signup coin
     * and if changename shows changeNameDialog */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        int id=item.getItemId();
        if(id == R.id.action_changemap && mapManager != null) {
            mapManager.updateMapType();
        }
        else if(id == R.id.action_logout) {
            lvHelper.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        else if (id == R.id.action_namechange) {
            changeNameDialog.show();
        }
        else if (id == R.id.action_post_box) {
            if(uiHelper.slidingPaneLayout.isOpen()) {
                uiHelper.slidingPaneLayout.closePane();
            }
            else {
                uiHelper.slidingPaneLayout.openPane();
            }
        }
        else if(id == R.id.action_about) {
            handleAbout();
        }
        else if(id == R.id.action_guide) {
            Intent intent = new Intent(this, GuideActivity.class);
            this.startActivity(intent);
        }
        return true;
    }

    /**
     * Initializes the dialog to change name(dynamically); if btn in it clicked run code in FBhelper.fgs to change name - method DB_changeName
     */
    public void InitChangeNameDialog() {
        Dialog d = new Dialog(this);
        LinearLayout l = new LinearLayout(d.getContext());
        EditText ed = new EditText(d.getContext());
        ed.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });
        ed.setInputType(InputType.TYPE_CLASS_TEXT);
        Button btn = new Button(d.getContext());
        btn.setOnClickListener(v -> {
            if(edtChangeName.getText().toString().equals("")) {
                edtChangeName.setError("Empty Field");
            }
            else {
                changeNameDialog.dismiss();
                lvHelper.getFirebaseHelper().fgs.DB_changeName(edtChangeName.getText().toString());
                setTitle(edtChangeName.getText().toString());
            }
        });
        btn.setText("Change Name");
        l.addView(ed);
        l.addView(btn);
        d.setContentView(l);

        changeNameDialog = d;
        edtChangeName = ed;
    }


    /** simply initializes the 3 variables and calls function to get async google-map */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        uiHelper = new UIHelper(this);
        lvHelper = new LVHelper(this, uiHelper);
        lvHelper.changeLVAdapter(LVHelper.LV_STATE.LV_STATE_GROUPS);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        InitChangeNameDialog();
    }

    /** if location permission enabled - enable my location in google map */
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 0) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mapManager.getGoogleMap().setMyLocationEnabled(true);
            }
        }
    }

    /** remove all listeners to firebase(in case we rotate screen listeners aren't removed, and onDestroy 100% called on rotation) */
    @Override
    protected void onDestroy() {
        lvHelper.removeAllListeners();
        super.onDestroy();
    }

    /** stop retrieving others locations and sharing mine location to others when outside of app, reset my location data. */
    @Override
    protected void onPause() {
        super.onPause();
        mapManager.keepThreadPause = false;
        mapManager.resetLocation();
        lvHelper.getFirebaseHelper().fls.DB_clearAllGroupsWithMineLocationAccess();
    }

    /**
     * if map initialized check if location getter thread is  still working, if yes do nothing if not put it on;
     * if were sending my location to firebase before onPause, resume it
     */
    @Override
    protected void onResume() {
        super.onResume();
        if(mapManager != null) { //first onResume always will be before map init
            uiHelper.chkAccessLocation.setChecked(false);
            mapManager.activityResumed();
            mapManager.considerStartLocationGetterThread();
        }
    }

    /** handles click: uiHelper.floatingPlus(to add group), uiHelper.floatingEye(hide listView), uiHelper.floatingArrowBack(change LVState
     * from showing users to showing groups), uiHelper.btnJoinLeaveGroup, uiHelper.btnOpenChat*/
    @Override
    public void onClick(View v) {
        if (v == uiHelper.floatingPlus) {
            lvHelper.addGroupWindow();
        } else if (v == uiHelper.floatingEye) {
            uiHelper.clickFloatingEye();
        } else if (v == uiHelper.floatingArrowBack) {
            uiHelper.clickFloatingArrowBack();
            lvHelper.changeLVAdapter(LVHelper.prevState);
            mapManager.clearMap();
        } else if (v == uiHelper.btnJoinLeaveGroup) {
            lvHelper.ADmanager.groupToJoinOrLeave = lvHelper.ADmanager.lastSelectedGroup;
            uiHelper.btnJoinLeaveGroup.setVisibility(View.GONE); //remove button
            if (uiHelper.btnJoinLeaveGroup.getText().toString().equals("Join")) {
                lvHelper.joinGroup();
            } else if (uiHelper.btnJoinLeaveGroup.getText().toString().equals("Leave")) {
                uiHelper.chkAccessLocation.setVisibility(View.GONE);
                uiHelper.btnOpenChat.setVisibility(View.GONE);
                lvHelper.leaveGroup();
            }
            else if (uiHelper.btnJoinLeaveGroup.getText().toString().equals("Request")) {
                lvHelper.system.requestJoinGroup();
            }
        }
        else if(v == uiHelper.btnOpenChat) {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("groupKey", lvHelper.ADmanager.lastSelectedGroup.key);
            intent.putExtra("groupName", lvHelper.ADmanager.lastSelectedGroup.name);
            startActivity(intent);
        }
    }


    /** if map ready create instance of mapManager */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        this.mapManager = new MapManager(this, googleMap, lvHelper.getFirebaseHelper(), service, uiHelper);
    }


    /** if performed longClick on listItem that is USER, then show dialog of possible actions;
     * if longClick on one of my groups, show dialog to change group settings */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_USERS) {
            GroupUser gu = lvHelper.ADmanager.lastSelectedGroup.groupUsers.get(position);
            if(gu.uid.equals(FirebaseHelper.current.uid)) {
                if(lvHelper.ADmanager.lastSelectedGroup.leaderUid.equals(FirebaseHelper.current.uid))
                    lvHelper.system.showLeaderGroupMailDialog();
                return true;
            }
            if(lvHelper.ADmanager.lastSelectedGroup.leaderUid.equals(FirebaseHelper.current.uid)) {
                lvHelper.system.showLeaderOptionsDialog(gu);
            }
            else {
                lvHelper.system.showParticipantOptionsDialog(gu);
            }
        }
        else if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_GROUPS || LVHelper.state == LVHelper.LV_STATE.LV_STATE_SEARCHED_GROUPS) {
            Group g = null;
            if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_GROUPS)
                g = lvHelper.ADmanager.getGroups().get(position);
            else if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_SEARCHED_GROUPS)
                g = lvHelper.ADmanager.getSearchedGroups().get(position);
            if(g.leaderUid.equals(FirebaseHelper.current.uid)) {
                lvHelper.updateGroupSettings(g);
            }
        }
        return true;
    }

    /** if clicked on group - show group's data+users; if clicked on user, move map camera to his location if visible */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_GROUPS || LVHelper.state == LVHelper.LV_STATE.LV_STATE_SEARCHED_GROUPS) {
            uiHelper.groupSelected();
            Group selected = null;
            if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_SEARCHED_GROUPS) { //if the group clicked is searched group
                selected = lvHelper.ADmanager.getSearchedGroups().get(position);
                //if im not already in this group and its not full enable join button
                if(lvHelper.ADmanager.getGroups().contains(selected) && FirebaseGroupSystem.canJoinLeaveGroup) {
                    uiHelper.updateJoinLeaveBtn(UIHelper.JoinLeaveBtnType.BTN_LEAVE);
                }
                else if(selected.participants < selected.maxParticipants && FirebaseGroupSystem.canJoinLeaveGroup) {
                    switch (selected.state) {
                        case GroupAdapter.GROUP_STATE_OPEN:
                            uiHelper.updateJoinLeaveBtn(UIHelper.JoinLeaveBtnType.BTN_JOIN);
                            break;
                        case GroupAdapter.GROUP_STATE_INVITE:
                            uiHelper.updateJoinLeaveBtn(UIHelper.JoinLeaveBtnType.BTN_INVITE);
                            break;
                        case GroupAdapter.GROUP_STATE_CLOSE:
                            uiHelper.btnJoinLeaveGroup.setVisibility(View.GONE);
                            break;
                    }
                }
                else {
                    uiHelper.btnJoinLeaveGroup.setVisibility(View.GONE);
                }
            }
            else if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_GROUPS){ //if its not searched groups just get from mine groups list
                selected = lvHelper.ADmanager.getGroups().get(position);
                if(FirebaseGroupSystem.canJoinLeaveGroup) {
                    uiHelper.updateJoinLeaveBtn(UIHelper.JoinLeaveBtnType.BTN_LEAVE);
                }
                else {
                    uiHelper.btnJoinLeaveGroup.setVisibility(View.GONE);
                }
            }

            if(lvHelper.ADmanager.lastSelectedGroup != null && !selected.key.equals(lvHelper.ADmanager.lastSelectedGroup.key)) {
                mapManager.getUsersLocationsMap().clear();
            }
            else
                mapManager.drawMapData();

            //update it with current group data
            uiHelper.tvGroupDescrip.setText(selected.name + System.getProperty("line.separator") +  "(" + selected.state + ")" + System.getProperty("line.separator") + System.getProperty("line.separator") + selected.description);

            //if our userAdapter doesn't has those users then fill it with the new one's
            if(selected != lvHelper.ADmanager.lastSelectedGroup) {
                lvHelper.ADmanager.lastSelectedGroup = selected;
                lvHelper.ADmanager.getUserAdapter().clear();
                lvHelper.ADmanager.getUserAdapter().addAll(selected.groupUsers);
            }
            LVHelper.prevState = LVHelper.state;
            LVHelper.state = LVHelper.LV_STATE.LV_STATE_USERS;
            uiHelper.lv.setAdapter(lvHelper.ADmanager.getUserAdapter()); //change adapter

            uiHelper.chkAccessLocation.setChecked(lvHelper.getFirebaseHelper().fls.canGroupAccessMineLocation());
            if(lvHelper.ADmanager.getGroups().contains(lvHelper.ADmanager.lastSelectedGroup) && FirebaseGroupSystem.canJoinLeaveGroup) {
                uiHelper.chkAccessLocation.setVisibility(View.VISIBLE);
                uiHelper.btnOpenChat.setVisibility(View.VISIBLE);
                AdapterManager.haveLastSelectedGroup = true;
            }
            else {
                uiHelper.btnOpenChat.setVisibility(View.GONE);
                AdapterManager.haveLastSelectedGroup = false;
            }
        }
        else if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_USERS) {
            if(lvHelper.ADmanager.getGroups().contains(lvHelper.ADmanager.lastSelectedGroup))
                mapManager.focusCamera(lvHelper.ADmanager.getUsers().get(position));
        }
    }
}