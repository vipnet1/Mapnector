package com.vippygames.mapnector.DBStorage;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class represents User as it is stored in the database
 */
@IgnoreExtraProperties
public class User {
    public String name;
    public String uid;
    public String key;
    /**
     * contains Strings that represent group keys that current user attached to
     */
    public ArrayList<String> myGroups;
    public ArrayList<Mail> myMails;
    /**
     * groups that user were kicked from and we still didn't leave them(for example user was kicked but app is closed)
     */
    public HashMap<String, String> groupsPendingKick;
    /**
     * groups that user asked to join and request accepted, and we still didn't join them(for example user was offline or app closed)
     */
    public HashMap<String, String> groupsPendingJoin;
    public double latitude, longitude; //300 if its null because its in values -180->180

    public User() {

    }

    public User(String name, String uid) {
        this.groupsPendingJoin = new HashMap<String, String>();
        this.groupsPendingKick = new HashMap<String, String>();
        this.name = name;
        this.uid = uid;
        this.latitude = 300;
        this.longitude = 300;
        myGroups = new ArrayList<String>();
        myMails = new ArrayList<Mail>();
    }

}
