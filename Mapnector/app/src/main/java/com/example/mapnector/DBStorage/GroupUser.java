package com.example.mapnector.DBStorage;

import com.google.firebase.database.IgnoreExtraProperties;

/**
In the database every group stores set of information about every user in it. this class represents this information.
 I did it because if we were storing 'User' objects the table were a lot bigger, and because we need a little information about the user
 compared to what is stored in User.class i made GroupUser class
 */
@IgnoreExtraProperties
public class GroupUser {
    public String uid;
    public String name;
    public String privilege;
    /**
     * whether this user allows CURRENT GROUP to access location data
     */
    public boolean allowAccessLoc;

    public GroupUser() {

    }

    public GroupUser(String uid, String name, String privilege, boolean allowAccessLoc) {
        this.uid = uid;
        this.name = name;
        this.privilege = privilege;
        this.allowAccessLoc = allowAccessLoc;
    }

    public GroupUser(String uid, String name, String privilege) {
        this.uid = uid;
        this.name = name;
        this.privilege = privilege;
        this.allowAccessLoc = false;
    }

    /**
     * two groupUsers are equal if the have the same uid(same groupUser cant appear twice in same group)
     * @param o object to compare
     * @return true if equals false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if(o.getClass() != GroupUser.class)
            return false;
        GroupUser g = (GroupUser)o;
        return this.uid.equals(g.uid);
    }

}
