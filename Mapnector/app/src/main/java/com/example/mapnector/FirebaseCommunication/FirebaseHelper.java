package com.example.mapnector.FirebaseCommunication;

import android.app.Activity;
import com.example.mapnector.DBStorage.User;
import com.example.mapnector.LVAdapters.AdapterManager;
import com.example.mapnector.UserInterface.UIHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * This class doe's almost nothing. it's aim is to manage and hold other Firebase classes so we can pass to other object only this one
 */
public class FirebaseHelper {
    public static FirebaseAuth mAuth;
    /**
     * reference to root in firebase
     */
    public static DatabaseReference refRoot;
    /**
     * reference to Users table in firebase
     */
    public static DatabaseReference refUsers;
    /**
     * reference to Group table in firebase
     */
    public static DatabaseReference refGroups;

    /**
     * instance of FirebaseGroupSystem, communicates with firebase with stuff associated with group management(join, leave, init etc.)
     */
    public FirebaseGroupSystem fgs;
    /**
     * instance of FirebaseLocationSystem, communicates with firebase with things that include location - retrieve location, get location etc
     */
    public FirebaseLocationSystem fls;

    /**
     * User.class object that represents the current user(me, the guy who just logged in); the value of this object is set in fgs.DB_InitCurrentUser and its one of the most important
     * variables of the app, so until we retrieve it we show dialog that not allows the user to do stuff in the app
     */
    public static User current;

    /**
     * initializes the constants and fgs,fls
     * @param ADmanager reference to AdapterManager created in LVHelper
     * @param uiHelper reference to uiHelper created in MapsActivtiy
     * @param activity MapsActivity
     */
    public FirebaseHelper(AdapterManager ADmanager, UIHelper uiHelper, Activity activity) {
        mAuth = FirebaseAuth.getInstance();
        refRoot = FirebaseDatabase.getInstance().getReference();
        refUsers = FirebaseDatabase.getInstance().getReference().child("Users");
        refGroups = FirebaseDatabase.getInstance().getReference().child("Groups");

        fgs = new FirebaseGroupSystem(ADmanager, uiHelper, activity);
        fls = new FirebaseLocationSystem(ADmanager);
    }

    /**
     * call mAuth.signOut()
     */
    public void logout() {mAuth.signOut();}
}
