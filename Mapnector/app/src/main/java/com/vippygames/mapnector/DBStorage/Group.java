package com.vippygames.mapnector.DBStorage;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;


/**
 * Represents a Group(In the table 'Groups') as it stored in the firebase
 */
@IgnoreExtraProperties
public class Group {
    public String name;
    public String description;
    public int participants;
    public int maxParticipants;
    public String key;
    public String leaderUid;
    public String state;

    public ArrayList<GroupUser> groupUsers;

    public Group() {

    }

    public Group(String key) {
        this.key = key;
    }

    public Group(String name, String description, String leaderUid, int maxParticipants, String state) {
        this.name = name;
        this.description = description;
        this.maxParticipants = maxParticipants;
        this.participants = 1;
        this.leaderUid = leaderUid;
        groupUsers = new ArrayList<GroupUser>();
        this.state = state;
    }

    /**
     * did it in order to be able to check whether group exists in group list via function - contains(that calls equals on every list element)
     * @param o the object to check if equals to current
     * @return true if objects equal, false otherwise(when i said that groups are equal if they have the same key)
     */
    @Override
    public boolean equals(Object o) {
        if(o.getClass() != Group.class) { //if not group, of course return false
            return false;
        }
        Group second = (Group)o;
        return this.key.equals(second.key); //if this group's key matches second one, they equal, cause db generates single key per group
    }

}
