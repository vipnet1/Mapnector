package com.vippygames.mapnector.GroupManagement;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.vippygames.mapnector.DBStorage.Group;
import com.vippygames.mapnector.DBStorage.GroupUser;
import com.vippygames.mapnector.FirebaseCommunication.FirebaseHelper;
import com.vippygames.mapnector.LVAdapters.AdapterManager;
import com.vippygames.mapnector.UserInterface.UIHelper;

/**
 * Class that acts as separator from MapsActivity to other important classes; It creates the FirebaseHelper, and holds other classes and functions; I think
 * that its one of the important classes because he manages the work of many areas in the project - for example of the firebase, ui and logic of what to do
 * in case user chose something; this class created many objects that references to them will be used by other classes(FBhelper, ADmanager etc)
 */
public class LVHelper implements View.OnClickListener {

    /**
     * Instance of AdapterManager that is created here
     */
    public AdapterManager ADmanager;
    /**
     * Reference to UIHelper created in MapsActivty
     */
    private final UIHelper uiHelper;
    /**
     * Instance of the CreateGroupSystem that is created here
     */
    private final CreateGroupSystem cgs;
    /**
     * Instance of the UserOptionsSystem, created in this class
     */
    public UserOptionsSystem system;
    /**
     * Instance of FirebaseHelper, created in this class
     */
    private final FirebaseHelper FBhelper;

    /**
     * what data left listView can hold
     */
    public enum LV_STATE {
        LV_STATE_NONE,
        LV_STATE_GROUPS,
        LV_STATE_USERS,
        LV_STATE_SEARCHED_GROUPS
    }

    /**
     * what left listView holds now
     */
    public static LV_STATE state;
    /**
     * what was the last thing the listView held before 'state'
     */
    public static LV_STATE prevState;

    /**
     * if want to update group settings via cgs, remember group key for future use
     */
    private String groupKeyToUpdate;

    /**
     * We initialize the variables and add listener to changes in the searchbar
     * @param activity MapsActitiy activity
     * @param uiHelper Reference to uiHelper in MapsActivity
     */
    public LVHelper(Activity activity, UIHelper uiHelper) {
        this.uiHelper = uiHelper;
        state = LV_STATE.LV_STATE_NONE;
        prevState = LV_STATE.LV_STATE_NONE;
        ADmanager = new AdapterManager(activity);
        FBhelper = new FirebaseHelper(ADmanager, uiHelper, activity);

        system = new UserOptionsSystem(activity, FBhelper, ADmanager);

        this.cgs = new CreateGroupSystem(activity);
        this.cgs.getBtnCreateGroup().setOnClickListener(this);

        uiHelper.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().equals(""))
                    changeLVAdapter(LV_STATE.LV_STATE_GROUPS);
                else
                    search();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * Remove all listeners to firebases, called in case onDestroy called(because if screen rotates listeners aren't destroyed)
     */
    public void removeAllListeners() {
        FBhelper.fgs.disableGroupListeners();
        FBhelper.fgs.disableSearchListener();
        FBhelper.fgs.disablePendingGroupsListeners();
        FBhelper.fgs.fms.disableMailListener();
    }

    /**
     * handles click on btnCreateGroup in the createGroupDialog(button located at cgs);
     *
     * @param v the clicked view
     */
    @Override
    public void onClick(View v) {
        if(v == cgs.getBtnCreateGroup()) {
            if(cgs.dialogVal == 0) {
                Group newGroup = cgs.createGroup();
                if(newGroup == null)
                    return;
                GroupUser me = new GroupUser(FirebaseHelper.current.uid, FirebaseHelper.current.name, "Leader");
                newGroup.groupUsers.add(me);
                FBhelper.fgs.DB_AddGroup(newGroup);
            }
            else if(cgs.dialogVal == 1) {
                FBhelper.fgs.DB_UpdateGroup(this.groupKeyToUpdate, cgs.getDescriptionText(), cgs.getStateText());
            }
            cgs.disableDialog();
        }
    }

    /**
     * Function to make it easier to change 'state' via switch-case
     * @param newState what left listView is going to hold now
     */
    //TODO: maximum efficiency function
    public void changeLVAdapter(LV_STATE newState) {
        if(state == newState)
            return;
        if(state == LV_STATE.LV_STATE_SEARCHED_GROUPS)
            FBhelper.fgs.disableSearchListener();
        state = newState;
        switch(newState) {
            case LV_STATE_GROUPS:
                uiHelper.lv.setAdapter(ADmanager.getGroupsAdapter());
                break;
            case LV_STATE_SEARCHED_GROUPS:
                uiHelper.lv.setAdapter(ADmanager.getSearchedGroupsAdapter());
                break;
            case LV_STATE_USERS:
                uiHelper.lv.setAdapter(ADmanager.getUserAdapter());
                break;
        }
    }

    /**
     * show dialog to create new group, request it from cgs
     */
    public void addGroupWindow() {
        cgs.showDialog();
    }

    /**
     * request firebase(calls DB_addSearchListener after clearing previous search) to add listener to all groups that their name is the text in the search bar
     */
    public void search() {
        ADmanager.getSearchedGroups().clear();
        uiHelper.lv.setAdapter(ADmanager.getSearchedGroupsAdapter());
        state = LV_STATE.LV_STATE_SEARCHED_GROUPS;
        FBhelper.fgs.DB_addSearchListener(uiHelper.edtSearch.getText().toString());
    }

    /**
     * ask the FBhelper.fgs to leave the group that is currently stored at ADmanager.lastSelectedGroup
     */
    public void leaveGroup() {
        FBhelper.fgs.DB_leaveGroup(FBhelper.fls.canGroupAccessMineLocation());
    }

    /**
     * ask the FBhelper.fgs to join the group that is currently stored at ADmanager.lastSelectedGroup
     */
    public void joinGroup() {
        FBhelper.fgs.DB_joinGroup();
    }

    /**
     * ask the firebase to delete the saved token for authentication
     */
    public void logout() {
        FBhelper.logout();
    }

    public FirebaseHelper getFirebaseHelper() {return FBhelper;}

    /**
     * show dialog to update group settings(same one as for creating group just with disabled fields)
     * @param g the group to update
     */
    public void updateGroupSettings(Group g) {
        this.groupKeyToUpdate = g.key;
        cgs.showUpdateDialog(g);
    }
}
