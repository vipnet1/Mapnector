package com.example.mapnector.LVAdapters;

import android.content.Context;

import com.example.mapnector.DBStorage.Group;
import com.example.mapnector.DBStorage.GroupUser;
import com.example.mapnector.FirebaseCommunication.FirebaseHelper;

import java.util.ArrayList;

/**
 * Class that's all its jon is to hold other adapters. It is passed as parameter to classes the need access to some adapters/arrayListst via it's get methods
 */
public class AdapterManager {
    private final ArrayList<Group> groups;
    private final ArrayList<GroupUser> users;
    private final ArrayList<Group> searchedGroups;

    private final GroupAdapter groupAdapter;
    private final GroupAdapter searchedGroupAdapter;
    private final UserAdapter userAdapter;
    private MailAdapter mailAdapter;

    /**
     * last clicked group in the left listView(no matter mine or from the searched ones)
     */
    public Group lastSelectedGroup; //last clicked group to view details
    /**
     * because while we request from firebase to join/leave group user can easily enter new groups we want to keep reference to the one we asked to join to;
     * the variable is used only by FirebaseGroupSystem
     */
    public Group groupToJoinOrLeave;

    /**
     * holds true if lastSelectedGroup is one of mine gropus and false otherwise
     */
    public static boolean haveLastSelectedGroup;

    /**
     * context of MapsActivity
     */
    private final Context context;


    /**
     * Initializes all the adapters and arrayLists except of the mailAdapter
     * @param context context of MapsActivity
     */
    public AdapterManager(Context context) {
        this.context = context;
        haveLastSelectedGroup = false;
        lastSelectedGroup = null;
        groupToJoinOrLeave = null;
        groups = new ArrayList<Group>();
        searchedGroups = new ArrayList<Group>();
        users = new ArrayList<GroupUser>();

        this.groupAdapter = new GroupAdapter(context, 0, 0, groups);
        this.searchedGroupAdapter = new GroupAdapter(context, 0, 0, searchedGroups);
        this.userAdapter = new UserAdapter(context, 0, 0, users);
    }

    public GroupAdapter getGroupsAdapter() {return this.groupAdapter;}
    public GroupAdapter getSearchedGroupsAdapter() {return this.searchedGroupAdapter;}
    public UserAdapter getUserAdapter() {return this.userAdapter;}
    public MailAdapter getMailAdapter() {return this.mailAdapter;}

    public ArrayList<Group> getGroups() {return this.groups;}
    public ArrayList<Group> getSearchedGroups() {return this.searchedGroups;}
    public ArrayList<GroupUser> getUsers() {return this.users;}

    /**
     * Initialize mailAdapter - because mailAdapter is the only one which takes his objects from FirebaseHelper.current.myMails we should wait
     * for FirebaseHelper to retrieve FirebaseHelper.current user from firebase before creating the adapter
     */
    public void initMailAdapter() {
        this.mailAdapter = new MailAdapter(context, 0, 0, FirebaseHelper.current.myMails);
    }
}
