package com.vippygames.mapnector.FirebaseCommunication;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vippygames.mapnector.DBStorage.Group;
import com.vippygames.mapnector.DBStorage.GroupUser;
import com.vippygames.mapnector.DBStorage.Mail;
import com.vippygames.mapnector.GroupManagement.UserOptionsSystem;
import com.vippygames.mapnector.LVAdapters.AdapterManager;
import com.vippygames.mapnector.LVAdapters.MailAdapter;
import com.vippygames.mapnector.UserInterface.UIHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Contains logic of doing actions associated with mails and the firebase;
 * One of the 3 FirebaseCommunication classes - manages work that has to do with the mail system
 */
public class FirebaseMailSystem {

    /**
     * ref to ADmanager created in LVhelper
     */
    private final AdapterManager ADmanager;
    /**
     * ref to current user's mailbox in firebase
     */
    private final DatabaseReference refMineMails;

    /**
     * listener to mine mail-box; run code when change in mails array occured
     */
    private final ValueEventListener mailListener;

    private final GenericTypeIndicator<List<Mail>> genericTypeIndicator;

    /**
     * arrayList of mails that should be removed(why array? suppose i want to remove one mail, second, mail, delete third... but i have no internet; this way all teh things
     * i asked to remove will be removed when the connection gets back
     */
    private final ArrayList<Mail> mailsToRemove;
    /**
     * dialog to decide whether to accept or reject requests to join some group
     */
    private final AlertDialog.Builder groupJoinRequestDialogBuilder;

    /**
     * the request to join group selected before opening dialog
     */
    private Mail selectedRequestMail;

    /**
     * reference to title of dialog to send custom mail in UserOptionsSystem.class; in order to allow send some mails without need the need of the user
     */
    private final EditText edtTitleSendMailRef;

    /**
     * types of mails that the app will create by itself without involving the user
     */
    public enum MAIL_TYPE {
        MAIL_TYPE_PROMOTED_LEADER,
        MAIL_TYPE_KICKED,
        MAIL_TYPE_INVITE_ACCEPTED
    }

    /**
     * a very long constructor(about 200 rows); at beginning initializes variables, then adds a lot of listeners; the main one's are that if we click on custom mail, we can write
     * a reply mail to the sender; if we long-click on some mail - the mail is removed from the mail list(deleted); and also adds simple mail listeners to updates in the firebase
     * @param ADmanager ref to ADmanager created in LVHelper
     * @param uiHelper ref to uiHelper created in MapsActivity
     * @param activity MapsActivity
     * @param edTitle edtTitle in dialog to create custom mail UserOptionsSystem.class
     */
    public FirebaseMailSystem(AdapterManager ADmanager, UIHelper uiHelper, Activity activity, EditText edTitle) {
        this.mailsToRemove = new ArrayList<Mail>();
        this.ADmanager = ADmanager;
        genericTypeIndicator = new GenericTypeIndicator<List<Mail>>() {};
        refMineMails = FirebaseHelper.refUsers.child(FirebaseHelper.current.key).child("myMails");
        this.edtTitleSendMailRef = edTitle;

        groupJoinRequestDialogBuilder = new AlertDialog.Builder(activity);
        groupJoinRequestDialogBuilder.setCancelable(true);

        groupJoinRequestDialogBuilder.setPositiveButton("Accept", (dialog, which) -> {
            mailsToRemove.add(FirebaseHelper.current.myMails.remove(FirebaseHelper.current.myMails.indexOf(selectedRequestMail)));
            ADmanager.getMailAdapter().notifyDataSetChanged();
            for(Group g : ADmanager.getGroups()) {
                if(g.key.equals(selectedRequestMail.groupKeyToRequest)) {
                    if(g.groupUsers.contains(new GroupUser(selectedRequestMail.senderUID, "idc", "idk"))) {
                        Toast.makeText(activity, "participant already in group", Toast.LENGTH_LONG).show();
                        return;
                    }
                    else
                        break;
                }
            }
            refMineMails.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    if(currentData.getValue() == null)
                        return Transaction.success(currentData);
                    if(mailsToRemove.isEmpty())
                        return Transaction.success(currentData);

                    List<Mail> list = currentData.getValue(genericTypeIndicator);
                    list.removeAll(mailsToRemove);
                    currentData.setValue(list);
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                    mailsToRemove.clear();
                }
            });

            FirebaseHelper.refUsers.orderByChild("uid").equalTo(selectedRequestMail.senderUID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for(DataSnapshot sp : snapshot.getChildren()) {
                        DatabaseReference ref = FirebaseHelper.refUsers.child(sp.getKey()).child("groupsPendingJoin").push();
                        ref.setValue(selectedRequestMail.groupKeyToRequest);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            DB_sendMail(selectedRequestMail.senderUID, MAIL_TYPE.MAIL_TYPE_INVITE_ACCEPTED);
        });
        groupJoinRequestDialogBuilder.setNegativeButton("Reject", (dialog, which) -> {
            mailsToRemove.add(FirebaseHelper.current.myMails.remove(FirebaseHelper.current.myMails.indexOf(selectedRequestMail)));
            ADmanager.getMailAdapter().notifyDataSetChanged();
            refMineMails.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    if(currentData.getValue() == null)
                        return Transaction.success(currentData);
                    if(mailsToRemove.isEmpty())
                        return Transaction.success(currentData);

                    List<Mail> list = currentData.getValue(genericTypeIndicator);
                    list.removeAll(mailsToRemove);
                    currentData.setValue(list);
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                    mailsToRemove.clear();
                }
            });
        });

        uiHelper.lvMails.setOnItemClickListener((parent, view, position, id) -> {
            Mail m = FirebaseHelper.current.myMails.get(position);
            if(m.event.equals(MailAdapter.MAIL_EVENT_CUSTOM_SINGLE)) {
                UserOptionsSystem.longClickSelected = new GroupUser(m.senderUID, m.from, "idc", false);
                UserOptionsSystem.isReplyMail = true;
                edtTitleSendMailRef.setText(m.title + "(Reply)");
                edtTitleSendMailRef.setEnabled(false);
                UserOptionsSystem.createCustomMailDialog.show();
            }
            else if(m.event.equals(MailAdapter.MAIL_EVENT_GROUP_JOIN_REQUEST)) {
                selectedRequestMail = m;
                groupJoinRequestDialogBuilder.setTitle(m.from + " wants to join one of your groups");
                groupJoinRequestDialogBuilder.setMessage("What's your action?");
                groupJoinRequestDialogBuilder.create().show();
            }
        });

        uiHelper.lvMails.setOnItemLongClickListener((parent, view, position, id) -> {
            mailsToRemove.add(FirebaseHelper.current.myMails.remove(position));
            ADmanager.getMailAdapter().notifyDataSetChanged();
            refMineMails.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    if(currentData.getValue() == null)
                        return Transaction.success(currentData);
                    if(mailsToRemove.isEmpty())
                        return Transaction.success(currentData);

                    List<Mail> list = currentData.getValue(genericTypeIndicator);
                    list.removeAll(mailsToRemove);
                    currentData.setValue(list);
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                    mailsToRemove.clear();
                }
            });
            return true;
        });

        //like hashMap, but without value. to check if elm exist in complexity O(1)
        //update db without spam mails
        mailListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    List<Mail> list = snapshot.getValue(genericTypeIndicator);

                    ADmanager.getMailAdapter().clear();
                    boolean shouldUpdateDB = false;
                    HashSet<String> set = new HashSet<String>(); //like hashMap, but without value. to check if elm exist in complexity O(1)
                    for (Mail m : list) {
                        if (m.event.equals(MailAdapter.MAIL_EVENT_GROUP_JOIN_REQUEST)) {
                            if (!set.contains(m.senderUID)) {
                                ADmanager.getMailAdapter().add(m);
                                set.add(m.senderUID);
                            } else
                                shouldUpdateDB = true;
                        } else
                            ADmanager.getMailAdapter().add(m);
                    }
                    if (shouldUpdateDB) { //update db without spam mails
                        refMineMails.runTransaction(new Transaction.Handler() {
                            @NonNull
                            @Override
                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                if (currentData.getValue() == null)
                                    return Transaction.success(currentData);

                                currentData.setValue(FirebaseHelper.current.myMails);
                                return Transaction.success(currentData);
                            }

                            @Override
                            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

                            }
                        });
                    }
                } else if (FirebaseHelper.current.myMails.size() == 1) {
                    FirebaseHelper.current.myMails.remove(0);
                    ADmanager.getMailAdapter().notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        refMineMails.addValueEventListener(mailListener);

    }

    /**
     * create's mail without involving the user in the process
     * @param type the type of the mail to create
     * @return the created mail
     */
    public Mail createMail(MAIL_TYPE type) {
        switch(type) {
            case MAIL_TYPE_PROMOTED_LEADER:
                return new Mail(FirebaseHelper.current.uid, "You Are The Leader!", "From: " + FirebaseHelper.current.name, MailAdapter.MAIL_EVENT_GOT_LEADERSHIP,
                        "You Are the new Leader of The group " + ADmanager.lastSelectedGroup.name);
            case MAIL_TYPE_KICKED:
                return new Mail(FirebaseHelper.current.uid, "You have been Kicked!", "From: " + FirebaseHelper.current.name, MailAdapter.MAIL_EVENT_KICK,
                        "You no longer participate in the group " + ADmanager.lastSelectedGroup.name);
            case MAIL_TYPE_INVITE_ACCEPTED:
                return new Mail(FirebaseHelper.current.uid, "Request to join group Accepted!", "From: " + FirebaseHelper.current.name, MailAdapter.MAIL_EVENT_GROUP_INVITE_ACCEPTED,
                        "You are now part of the group");
        }
        return null;
    }

    /**
     * one of 3 functions to send mails to some user in firebase; this one is used often by mails that aren't involving the user
     * @param guUid the groupUser to send mail to
     * @param type the type of the mail that should be created by app automatically
     */
    public void DB_sendMail(String guUid, MAIL_TYPE type){
        FirebaseHelper.refUsers.orderByChild("uid").equalTo(guUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot sp : snapshot.getChildren()) {
                    FirebaseHelper.refUsers.child(sp.getKey()).child("myMails").runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            currentData.getChildrenCount();
                            Mail m = createMail(type);
                            currentData.child(String.valueOf(currentData.getChildrenCount())).setValue(m);
                            return Transaction.success(currentData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * One of the 3 functions to send mail; here we send one mail to all groupUsers of some group; used often by leaders who want to send one mail to all group members(without
     * the leader himself of course)
     * @param mail the mail to send - created by CreateGroupSystem
     */
    public void DB_sendMail(Mail mail){
        for(GroupUser gu : ADmanager.lastSelectedGroup.groupUsers) {
            if(!gu.uid.equals(FirebaseHelper.current.uid)) {
                FirebaseHelper.refUsers.orderByChild("uid").equalTo(gu.uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot sp : snapshot.getChildren()) {
                            FirebaseHelper.refUsers.child(sp.getKey()).child("myMails").runTransaction(new Transaction.Handler() {
                                @NonNull
                                @Override
                                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                    currentData.getChildrenCount();
                                    currentData.child(String.valueOf(currentData.getChildrenCount())).setValue(mail);
                                    return Transaction.success(currentData);
                                }

                                @Override
                                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }
    }

    public void disableMailListener() {
        refMineMails.removeEventListener(mailListener);
    }


    /**
     * One of the 3 functions to send mail; send mail to a specific groupUser; the mail created via CreateGroupSystem;
     * @param guUid the groupUser uid to send the mail
     * @param mail the mail to send - created by CreateGroupSystem
     */
    public void DB_sendMail(String guUid, Mail mail){
        FirebaseHelper.refUsers.orderByChild("uid").equalTo(guUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot sp : snapshot.getChildren()) {
                    FirebaseHelper.refUsers.child(sp.getKey()).child("myMails").runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            currentData.getChildrenCount();
                            currentData.child(String.valueOf(currentData.getChildrenCount())).setValue(mail);
                            return Transaction.success(currentData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}