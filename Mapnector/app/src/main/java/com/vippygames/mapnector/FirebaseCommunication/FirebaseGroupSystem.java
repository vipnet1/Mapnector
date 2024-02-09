package com.vippygames.mapnector.FirebaseCommunication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vippygames.mapnector.DBStorage.Group;
import com.vippygames.mapnector.DBStorage.GroupUser;
import com.vippygames.mapnector.DBStorage.Mail;
import com.vippygames.mapnector.DBStorage.User;
import com.vippygames.mapnector.GroupManagement.LVHelper;
import com.vippygames.mapnector.LVAdapters.AdapterManager;
import com.vippygames.mapnector.LVAdapters.GroupAdapter;
import com.vippygames.mapnector.R;
import com.vippygames.mapnector.UserInterface.UIHelper;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * in my opinion the most important class in the application. Contains logic of doing many actions on groups, users, initializes FirebaseHelper.current;
 * One of the 3 FirebaseCommunication classes - manages work that has to do with groups management and users. Stuff that has to do with mails goes to be processed in
 * the son, fms
 */
public class FirebaseGroupSystem {
    /**
     * reference to array of mine groups in firebase
     */
    private DatabaseReference refMineGroups;
    /**
     * reference to array of groups that i should be kicked from in firebase; most of the time empty(not existing in firebase)
     */
    private DatabaseReference refMineGroupsPendingKick;
    /**
     * reference to array of groups that i should join to(request to join accepted) in firebase; most of the time empty(not existing in firebase)
     */
    private DatabaseReference refMineGroupsPendingJoin;
    /**
     * reference to a special place in firebase that if we listen to it we can run code when app connected to firebase(includes internet connection + to firebase in general)
     */
    private final DatabaseReference connectedRef;

    /**
     * reference to the ADmanager created in LVHelper
     */
    private final AdapterManager ADmanager;

    /**
     * listener to all groups with name specified in edtSearch; puts in left listView the new groups every time
     */
    private final ValueEventListener searchListener;
    /**
     * listener to mine groups; update current group data in the ui
     */
    private final ValueEventListener groupListener;
    /**
     * listener to connection to firebase changes event
     */
    private final ValueEventListener connectionListener;
    /**
     * listener to groupsPendingKick; clear the array and remove myself from the groups specified there
     */
    private final ValueEventListener groupsPendingKickListener;
    /**
     * listener to groupsPendingJoin; clear the array and add myself to groups specified there
     */
    private final ValueEventListener groupsPendingJoinListener;
    /**
     * last String typed in edtSearch
     */
    private String lastSearch;

    /**
     * reference to UIHelper
     */
    private final UIHelper uiHelper;

    /**
     * progressBar to not allow to work in app until we initialize FirebaseHelper.current(current user)
     */
    private final ProgressDialog pdSearchingConnection;

    /**
     * progressBar to not allow the user to change his name again before the old action didn't complete(simply blocks from app till we get onComplete from transaction)
     */
    private final ProgressDialog pdWaitingNamechange;

    /**
     * to show useful messages
     */
    private final Snackbar snackbar;
    private final String msg1 = "Group Is Full";
    private final String msg2 = "Group Doesn't Exists";
    private final String msg3 = "You Don't Participate in the group anymore!";
    private final String msg4 = "Before leaving give leadership to one of the group participants!";

    /**
     * indicates if we can join or leave or do some action to an group; suppose we asked to join group and then internet goes - we don'w want to request to leave/join
     * another group until we get response from firebase and then we open this opportunity; true means can join/leave otherwise false
     */
    public static boolean canJoinLeaveGroup;
    /**
     * true if im the last in the group i will try to leave now; suppose im the last in group - i want to leave it and so delete all chat data so noeone can view it;
     * suppose i press button to leave group but someone else joins immediatly after i press it - in that case i will not leave the group, mine request will be ignored
     */
    private boolean isLastInGroup;

    /**
     * MapsActivtiy
     */
    private final Activity activity;

    /**
     * FirebaseMailSystem; part that manages stuff that has to do with mails
     */
    public FirebaseMailSystem fms;
    /**
     * reference to editText(title) in sendMail dialog, in order to pass it to fms later
     */
    private EditText edtTitleSendMailRef;


    /**
     * we initialize most of the variables and all the listeners, we call DB_InitCurrentUser
     * @param ADmanager ref to ADmanager
     * @param uiHelper ref to UIHelper
     * @param activity ref to MapsActivity
     */
    public FirebaseGroupSystem(AdapterManager ADmanager, UIHelper uiHelper, Activity activity) {
        this.ADmanager = ADmanager;
        this.uiHelper = uiHelper;
        this.activity = activity;
        pdSearchingConnection = new ProgressDialog(activity);
        pdSearchingConnection.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.parseColor("#FF7B7B")));
        pdSearchingConnection.setMessage("Searching For Connection to the Internet...");
        pdSearchingConnection.setCancelable(false);

        pdWaitingNamechange = new ProgressDialog(activity);
        pdWaitingNamechange.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.parseColor("#E6F622")));
        pdWaitingNamechange.setMessage("Changing Your Name...");
        pdWaitingNamechange.setCancelable(false);

        connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");

        connectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    pdSearchingConnection.dismiss();
                    connectedRef.removeEventListener(connectionListener);
                } else {
                    pdSearchingConnection.show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        connectedRef.addValueEventListener(connectionListener);

        canJoinLeaveGroup = true;
        snackbar = Snackbar.make(activity.findViewById(R.id.mainLayout),msg1, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("Ok", v -> {

        });

        searchListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean foundCurrGroup = false;
                ADmanager.getSearchedGroupsAdapter().clear(); //clear last search info
                for(DataSnapshot sp : snapshot.getChildren()) { //run on all search results
                    Group gr = sp.getValue(Group.class); //get the group
                    ADmanager.getSearchedGroupsAdapter().add(gr); //set in list
                    if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_USERS && ADmanager.lastSelectedGroup.key.equals(gr.key)) {
                        foundCurrGroup = true;
                        ADmanager.lastSelectedGroup = gr;
                        ADmanager.getUserAdapter().clear();
                        ADmanager.getUserAdapter().addAll(gr.groupUsers);
                        uiHelper.tvGroupDescrip.setText(gr.name + System.getProperty("line.separator") +  "(" + gr.state + ")" + System.getProperty("line.separator") + System.getProperty("line.separator") + gr.description);
                        if(canJoinLeaveGroup && !ADmanager.getGroups().contains(gr)) {
                            if(gr.participants >= gr.maxParticipants || gr.state.equals(GroupAdapter.GROUP_STATE_CLOSE))
                                uiHelper.btnJoinLeaveGroup.setVisibility(View.GONE);
                            else if(gr.state.equals(GroupAdapter.GROUP_STATE_OPEN)) {
                                uiHelper.updateJoinLeaveBtn(UIHelper.JoinLeaveBtnType.BTN_JOIN);
                                uiHelper.btnJoinLeaveGroup.setVisibility(View.VISIBLE);
                            }
                            else if(gr.state.equals(GroupAdapter.GROUP_STATE_INVITE)) {
                                uiHelper.updateJoinLeaveBtn(UIHelper.JoinLeaveBtnType.BTN_INVITE);
                                uiHelper.btnJoinLeaveGroup.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
                if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_USERS && !foundCurrGroup) { //means that group doesn't exists anymore
                    ADmanager.getUserAdapter().clear();
                    uiHelper.btnJoinLeaveGroup.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        //TODO: maximum efficiency code
        groupListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Group g = snapshot.getValue(Group.class);
                int idx = ADmanager.getGroupsAdapter().getPosition(g);
                if(idx != -1) { //to insert in same place it was before
                    ADmanager.getGroupsAdapter().remove(g);
                    ADmanager.getGroupsAdapter().insert(g, idx);
                }
                else {
                    ADmanager.getGroupsAdapter().add(g);
                }
                if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_USERS && ADmanager.lastSelectedGroup.key.equals(g.key)) {
                    ADmanager.getUserAdapter().clear();
                    ADmanager.getUserAdapter().addAll(g.groupUsers);
                    uiHelper.tvGroupDescrip.setText(g.name + System.getProperty("line.separator") +  "(" + g.state + ")" + System.getProperty("line.separator") + System.getProperty("line.separator") + g.description);
                    ADmanager.lastSelectedGroup = g;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        groupsPendingKickListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                refMineGroupsPendingKick.runTransaction(new Transaction.Handler() {
                    boolean shouldDisableUserViews = false;
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        if(currentData.getValue() == null)
                            return Transaction.success(currentData);
                        for(MutableData child : currentData.getChildren()) {
                            String groupKey = child.getValue(String.class);
                            FirebaseHelper.refGroups.child(groupKey).removeEventListener(groupListener);
                            ADmanager.getGroups().remove(new Group(groupKey));
                            FirebaseHelper.current.myGroups.remove(groupKey);
                            if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_USERS && ADmanager.lastSelectedGroup != null && ADmanager.lastSelectedGroup.key.equals(groupKey)) {
                                shouldDisableUserViews = true;
                            }
                            FirebaseHelper.refGroups.child(groupKey).removeEventListener(groupListener);
                        }
                        currentData.setValue(null);
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                        if(shouldDisableUserViews) {
                            uiHelper.btnOpenChat.setVisibility(View.GONE);
                            uiHelper.btnJoinLeaveGroup.setVisibility(View.GONE);
                            uiHelper.chkAccessLocation.setVisibility(View.GONE);
                        }
                        ADmanager.getGroupsAdapter().notifyDataSetChanged();
                        refMineGroups.runTransaction(new Transaction.Handler() {
                            @NonNull
                            @Override
                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                currentData.setValue(FirebaseHelper.current.myGroups);
                                return Transaction.success(currentData);
                            }

                            @Override
                            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

                            }
                        });
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        groupsPendingJoinListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                refMineGroupsPendingJoin.runTransaction(new Transaction.Handler() {
                    boolean disableRequestBtn = false;

                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        if(currentData.getValue() == null)
                            return Transaction.success(currentData);
                        for(MutableData child : currentData.getChildren()) {
                            String groupKey = child.getValue(String.class);
                            if(ADmanager.lastSelectedGroup != null && ADmanager.lastSelectedGroup.key.equals(groupKey) && LVHelper.state == LVHelper.LV_STATE.LV_STATE_USERS) {
                                disableRequestBtn = true;
                            }
                            DB_joinGroup(groupKey);
                        }
                        currentData.setValue(null);
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                        if(committed && disableRequestBtn) {
                            uiHelper.btnJoinLeaveGroup.setVisibility(View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        this.DB_InitCurrentUser();
    }


    /**
     * we retrieve current user data from firebase, and initialize references(DatabaseReference); we add listeners to groups that i participate in; we load the mails i have gotten
     * so far; we add listeners to mine groupsPendingKick, groupsPendingJoin to leave/join them in case they aren't null in firebase; We init fms.
     */
    //TODO: maximum efficiency function
    private void DB_InitCurrentUser() {
        FirebaseHelper.refUsers.orderByChild("uid").equalTo(FirebaseHelper.mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot sp : snapshot.getChildren()) {
                    FirebaseHelper.current = sp.getValue(User.class);
                    activity.setTitle("Welcome " + FirebaseHelper.current.name + "!");
                    if(FirebaseHelper.current.groupsPendingKick == null)
                        FirebaseHelper.current.groupsPendingKick = new HashMap<String, String>();
                    if(FirebaseHelper.current.myGroups == null) { //if im not in groups yet and have no arrayList for them create one
                        FirebaseHelper.current.myGroups = new ArrayList<String>();
                    }
                    refMineGroups = FirebaseHelper.refUsers.child(FirebaseHelper.current.key).child("myGroups");
                    refMineGroupsPendingKick = FirebaseHelper.refUsers.child(FirebaseHelper.current.key).child("groupsPendingKick");
                    refMineGroupsPendingJoin = FirebaseHelper.refUsers.child(FirebaseHelper.current.key).child("groupsPendingJoin");

                    for(String groupKey : FirebaseHelper.current.myGroups) {
                        FirebaseHelper.refGroups.child(groupKey).addValueEventListener(groupListener);
                    }

                    refMineGroupsPendingKick.addValueEventListener(groupsPendingKickListener);
                    refMineGroupsPendingJoin.addValueEventListener(groupsPendingJoinListener);

                }
                if(FirebaseHelper.current.myMails == null)
                    FirebaseHelper.current.myMails = new ArrayList<Mail>();
                ADmanager.initMailAdapter();
                fms = new FirebaseMailSystem(ADmanager, uiHelper, activity, edtTitleSendMailRef);
                uiHelper.lvMails.setAdapter(ADmanager.getMailAdapter());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void setEdtTitleSendMailRef(EditText ref) {
        this.edtTitleSendMailRef = ref;
    }

    /**
     * kick a groupUser from the group; for security stuff i decided that none except of the user can't view or update 'myGroups' table in firebase; so we add  current group to
     *  groupsPendingKick of the groupUser and he will "kick himself" next time he restarts the app; data about user immediatly cleaned from group - but to clear data about the
     *   group from the user we will let the user do it by himself;
     * @param gu the groupUser to kick
     */
    public void DB_kickUser(GroupUser gu) {
        FirebaseHelper.refGroups.child(ADmanager.lastSelectedGroup.key).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Group g = currentData.getValue(Group.class);
                if(g == null)
                    return Transaction.success(currentData);
                if(!g.groupUsers.remove(gu)) {
                    return Transaction.abort();
                }
                g.participants--;
                currentData.setValue(g);
                fms.DB_sendMail(gu.uid, FirebaseMailSystem.MAIL_TYPE.MAIL_TYPE_KICKED);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                FirebaseHelper.refUsers.orderByChild("uid").equalTo(gu.uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot sp : snapshot.getChildren()) {
                            DatabaseReference ref = FirebaseHelper.refUsers.child(sp.getKey()).child("groupsPendingKick").push();
                            ref.setValue(ADmanager.lastSelectedGroup.key);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
    }

    /**
     * make the groupUser leader of the group, and me a participant; send to that guy a mail that he got leadership
     * @param gu the groupUser to give leadership to
     */
    public void DB_giveLeadership(GroupUser gu) {

        FirebaseHelper.refGroups.child(ADmanager.lastSelectedGroup.key).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Group g = currentData.getValue(Group.class);
                if(g==null)
                    return Transaction.success(currentData);

                gu.privilege = "Leader";
                int idx = g.groupUsers.indexOf(gu);
                g.groupUsers.remove(gu);
                g.groupUsers.add(idx, gu);

                GroupUser fakeMe = new GroupUser(FirebaseHelper.current.uid, "idc", "none");
                idx = g.groupUsers.indexOf(fakeMe);
                GroupUser meReal = g.groupUsers.get(idx);
                meReal.privilege = "Participant";
                g.groupUsers.remove(meReal);
                g.groupUsers.add(idx, meReal);
                g.leaderUid = gu.uid;
                currentData.setValue(g);
                fms.DB_sendMail(gu.uid, FirebaseMailSystem.MAIL_TYPE.MAIL_TYPE_PROMOTED_LEADER);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

            }
        });
    }

    /**
     * add the group created by CreateGroupSystem to the list of groups in firebase
     * @param g the group just created and that have to be inserted in firebase
     */
    //TODO: maximum efficiency function
    public void DB_AddGroup(Group g) {
        DatabaseReference newGroupRef = FirebaseHelper.refGroups.push();
        g.key = newGroupRef.getKey();
        FirebaseHelper.refGroups.child(g.key).setValue(g);
        refMineGroups.child(String.valueOf(FirebaseHelper.current.myGroups.size())).setValue(g.key);
        FirebaseHelper.current.myGroups.add(g.key);
        FirebaseHelper.refGroups.child(g.key).addValueEventListener(groupListener);
    }


    /**
     * join the group with 'groupKey' key - that means add myself to groupUsers list of that group, update the participant number, add the group to myGroups list;
     * That function(with parameter) used when we need to add group by key from groupsPendingJoin, more often we use the function without params
     * @param groupKey the key of the groups to be added
     */
    public void DB_joinGroup(String groupKey) {
        canJoinLeaveGroup = false;
        //only i can read and set data to this group, to avoid race condition
        FirebaseHelper.refGroups.child(groupKey).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {

                Group g = currentData.getValue(Group.class);
                if(g==null)
                    return Transaction.success(currentData);

                //if we clicked on join btn, but someone went in before us and filled the group so we cant join
                if(g.participants >= g.maxParticipants) {
                    snackbar.setText(msg1);
                    snackbar.show();
                    return Transaction.abort();
                }
                GroupUser me = new GroupUser(FirebaseHelper.current.uid, FirebaseHelper.current.name, "Participant"); //create new groupUser(me)
                ADmanager.groupToJoinOrLeave = g;
                ADmanager.groupToJoinOrLeave.participants++;
                ADmanager.groupToJoinOrLeave.groupUsers.add(me);

                currentData.setValue(ADmanager.groupToJoinOrLeave);

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

                //we want to add listener to this group, in case someone joins/leaves after me
                if(committed) {
                    if(ADmanager.lastSelectedGroup != null && ADmanager.lastSelectedGroup.key.equals(ADmanager.groupToJoinOrLeave.key)) {
                        AdapterManager.haveLastSelectedGroup = true;
                        if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_USERS) {
                            uiHelper.btnOpenChat.setVisibility(View.VISIBLE);
                            uiHelper.chkAccessLocation.setVisibility(View.VISIBLE);
                        }
                    }

                    FirebaseHelper.current.myGroups.add(ADmanager.groupToJoinOrLeave.key);
                    FirebaseHelper.refUsers.child(FirebaseHelper.current.key).child("myGroups").setValue(FirebaseHelper.current.myGroups);
                    FirebaseHelper.refGroups.child(ADmanager.groupToJoinOrLeave.key).addValueEventListener(groupListener);
                }
                canJoinLeaveGroup = true;
            }
        });
    }

    /**
     * join the group ADmanager.lastSelectedGroup - that means add myself to groupUsers list of that group, update the participant number, add the group to myGroups list;
     */
    public void DB_joinGroup() {
        canJoinLeaveGroup = false;
        //only i can read and set data to this group, to avoid race condition
        FirebaseHelper.refGroups.child(ADmanager.lastSelectedGroup.key).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {

                Group g = currentData.getValue(Group.class);
                if(g == null) { //means group was deleted
                    snackbar.setText(msg2);
                    snackbar.show();
                    return Transaction.abort();
                }
                //if we clicked on join btn, but someone went in before us and filled the group so we cant join
                if(g.participants >= g.maxParticipants) {
                    snackbar.setText(msg1);
                    snackbar.show();
                    return Transaction.abort();
                }
                GroupUser me = new GroupUser(FirebaseHelper.current.uid, FirebaseHelper.current.name, "Participant"); //create new groupUser(me)
                ADmanager.groupToJoinOrLeave = g;
                ADmanager.groupToJoinOrLeave.participants++;
                ADmanager.groupToJoinOrLeave.groupUsers.add(me);

                if(ADmanager.lastSelectedGroup.key.equals(ADmanager.groupToJoinOrLeave.key))
                    ADmanager.lastSelectedGroup = ADmanager.groupToJoinOrLeave;

                currentData.setValue(ADmanager.groupToJoinOrLeave);

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                //we want to add listener to this group, in case someone joins/leaves after me
                if(committed) {
                    AdapterManager.haveLastSelectedGroup = true;
                    if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_USERS && ADmanager.lastSelectedGroup.equals(ADmanager.groupToJoinOrLeave)) {
                        uiHelper.btnOpenChat.setVisibility(View.VISIBLE);
                        uiHelper.chkAccessLocation.setVisibility(View.VISIBLE);
                    }
                    FirebaseHelper.current.myGroups.add(ADmanager.groupToJoinOrLeave.key);
                    FirebaseHelper.refUsers.child(FirebaseHelper.current.key).child("myGroups").setValue(FirebaseHelper.current.myGroups);
                    FirebaseHelper.refGroups.child(ADmanager.groupToJoinOrLeave.key).addValueEventListener(groupListener);
                }
                canJoinLeaveGroup = true;
            }
        });
    }

    /**
     * leave group that ADmanager.lastSelectedGroup; checks many "special cases" for example
     * in case im leader and more than one participant is in the group i cant leave - must give someone leadership
     * @param groupHaveLocationAccess whether allowAccessLoc of the group in firebase set to true or false, in order to know if to enable or not checkbox if leave canceled
     */
    public void DB_leaveGroup(boolean groupHaveLocationAccess) {
        if(ADmanager.lastSelectedGroup.leaderUid.equals(FirebaseHelper.current.uid) && ADmanager.lastSelectedGroup.participants > 1) {
            uiHelper.btnOpenChat.setVisibility(View.VISIBLE);
            uiHelper.btnJoinLeaveGroup.setVisibility(View.VISIBLE);
            uiHelper.chkAccessLocation.setVisibility(View.VISIBLE);
            snackbar.setText(msg4);
            snackbar.show();
            return;
        }
        canJoinLeaveGroup = false;
        if(ADmanager.groupToJoinOrLeave.participants == 1)
            isLastInGroup = true;
        else
            isLastInGroup = false;
        FirebaseHelper.refGroups.child(ADmanager.groupToJoinOrLeave.key).removeEventListener(groupListener);

        FirebaseHelper.refGroups.child(ADmanager.groupToJoinOrLeave.key).runTransaction(new Transaction.Handler() {
            int abortReason; //1-means cause someone just joined and don't want to leave. 2-i already left group(got kicked out)
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Group g = currentData.getValue(Group.class);
                if(g == null)
                    return Transaction.success(currentData);
                if(!g.groupUsers.contains(new GroupUser(FirebaseHelper.current.uid, "idc", "none"))) {
                    abortReason = 2;
                    return Transaction.abort();
                }
                if(g.participants > 1 && isLastInGroup) {
                    abortReason = 1;
                    return Transaction.abort();
                }
                GroupUser me = new GroupUser(FirebaseHelper.current.uid, "idc", "none");
                ADmanager.groupToJoinOrLeave = g;
                ADmanager.groupToJoinOrLeave.participants--;
                ADmanager.groupToJoinOrLeave.groupUsers.remove(me);

                if(ADmanager.groupToJoinOrLeave.key.equals(ADmanager.lastSelectedGroup.key))
                    ADmanager.lastSelectedGroup = ADmanager.groupToJoinOrLeave;

                if(ADmanager.groupToJoinOrLeave.participants == 0)
                    currentData.setValue(null);
                else
                    currentData.setValue(ADmanager.groupToJoinOrLeave);

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if(committed) {
                    if(isLastInGroup) {
                        FirebaseHelper.refRoot.child("Chats").child(ADmanager.groupToJoinOrLeave.key).setValue(null);
                    }
                    FirebaseHelper.current.myGroups.remove(ADmanager.groupToJoinOrLeave.key);
                    FirebaseHelper.refUsers.child(FirebaseHelper.current.key).child("myGroups").setValue(FirebaseHelper.current.myGroups);

                    AdapterManager.haveLastSelectedGroup = false;
                    ADmanager.getGroupsAdapter().remove(ADmanager.groupToJoinOrLeave);
                    if(LVHelper.prevState == LVHelper.LV_STATE.LV_STATE_GROUPS && ADmanager.groupToJoinOrLeave.equals(ADmanager.lastSelectedGroup)) {
                        GroupUser me = new GroupUser(FirebaseHelper.current.uid, "idc", "none");
                        ADmanager.getUserAdapter().remove(me);
                    }
                }
                else {
                    if(abortReason == 1) {
                        FirebaseHelper.refGroups.child(ADmanager.groupToJoinOrLeave.key).addValueEventListener(groupListener);
                        if(LVHelper.state == LVHelper.LV_STATE.LV_STATE_USERS && ADmanager.lastSelectedGroup.equals(ADmanager.groupToJoinOrLeave)) {
                            uiHelper.btnOpenChat.setVisibility(View.VISIBLE);
                            uiHelper.chkAccessLocation.setVisibility(View.VISIBLE);
                            uiHelper.chkAccessLocation.setChecked(groupHaveLocationAccess);
                            uiHelper.updateJoinLeaveBtn(UIHelper.JoinLeaveBtnType.BTN_LEAVE);
                        }
                    }
                    else if(abortReason == 2) {
                        snackbar.setText(msg3);
                        snackbar.show();
                    }
                }
                canJoinLeaveGroup = true;
            }
        });
    }

    /**
     * add listener to all groups that their name is the parameter; its made so we can view all groups that user types in edtSearch in left listView
     * @param searchedVal the name of the groups to listen to
     */
    public void DB_addSearchListener(String searchedVal) {
        FirebaseHelper.refGroups.orderByChild("name").equalTo(lastSearch).removeEventListener(searchListener);
        lastSearch = searchedVal;
        FirebaseHelper.refGroups.orderByChild("name").equalTo(searchedVal).addValueEventListener(searchListener);
    }

    /**
     * remove listeners for all groups with name 'lastSearch'
     */
    public void disableSearchListener() {
        FirebaseHelper.refGroups.orderByChild("name").equalTo(lastSearch).removeEventListener(searchListener);
        lastSearch = "";
    }

    /**
     * remove listeners for groupsPendingKick, groupsPendingJoin. in case screen rotate we can accidently run sam code twice so we clear previous listeners
     */
    public void disablePendingGroupsListeners() {
        refMineGroupsPendingKick.removeEventListener(groupsPendingKickListener);
        refMineGroupsPendingJoin.removeEventListener(groupsPendingJoinListener);
    }


    /**
     * remove group listeners to groups im participating in
     */
    public void disableGroupListeners() {
        for(Group g : ADmanager.getGroups()) {
            FirebaseHelper.refGroups.child(g.key).removeEventListener(groupListener);
        }
    }

    /**
     * we change the name in the Users table of the current user and in groupUsers table of each group in firebase; we don't touch chat system and mails
     * so they will remain with the old name(but this will work because all the actions are done by user uid and not name, for example reply mail done via uid)
     * @param name the new name of the user
     */
    public void DB_changeName(String name) {
        pdWaitingNamechange.show();
        FirebaseHelper.current.name = name;
        FirebaseHelper.refUsers.child(FirebaseHelper.current.key).child("name").setValue(name);

        for(String groupKey : FirebaseHelper.current.myGroups) {
            FirebaseHelper.refGroups.child(groupKey).runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    if(currentData.getValue() == null)
                        return Transaction.success(currentData);
                    Group g = currentData.getValue(Group.class);
                    int idx = g.groupUsers.indexOf(new GroupUser(FirebaseHelper.current.uid, "idc", "idc"));
                    currentData.child("groupUsers").child(String.valueOf(idx)).child("name").setValue(name);
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                    pdWaitingNamechange.dismiss();
                }
            });
        }
    }

    /**
     * update group data in firebase; called after the btnCreateGroup of the dialog clicked; we change only the description and the state of the group
     * @param key the key of the group to chagne
     * @param descrip the new description of the group
     * @param state the new state o the group(closed, opened or invitation)
     */
    public void DB_UpdateGroup(String key, String descrip, String state) {
        FirebaseHelper.refGroups.child(key).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if(currentData.getValue() == null)
                    return Transaction.success(currentData);
                currentData.child("description").setValue(descrip);
                currentData.child("state").setValue(state);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

            }
        });
    }
}
