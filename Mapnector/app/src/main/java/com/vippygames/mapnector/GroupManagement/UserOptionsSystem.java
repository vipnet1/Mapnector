package com.vippygames.mapnector.GroupManagement;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.vippygames.mapnector.DBStorage.GroupUser;
import com.vippygames.mapnector.DBStorage.Mail;
import com.vippygames.mapnector.FirebaseCommunication.FirebaseHelper;
import com.vippygames.mapnector.LVAdapters.AdapterManager;
import com.vippygames.mapnector.LVAdapters.MailAdapter;
import com.vippygames.mapnector.R;

public class UserOptionsSystem implements View.OnClickListener {
    /**
     * reference to FBhelper created in ListView
     */
    private final FirebaseHelper FBhelper;
    /**
     * reference to ADmanager created in ListView
     */
    private final AdapterManager ADmanager;

    /**
     * dialog that shown as last step before giving someone leadership - to ask user verify that its the wanted action
     */
    private final AlertDialog alertDialogGiveLeadership;
    /**
     * show dialog of actions that participant in group can do when long-clicking other group users
     */
    private final AlertDialog.Builder chooseParticipantActionBuilder;
    /**
     * show dialog of actions that leader(me) in group can do when long-clicking other group users
     */
    private final AlertDialog.Builder chooseLeaderActionBuilder;
    /**
     * show dialog of actions that leader(me) in group can do when long-clicking himself(send email to all group users)
     */
    private final AlertDialog.Builder sendGroupEmailActionBuilder;
    /**
     * show dialog to create custom mail to send to someone - with title, body
     */
    public static Dialog createCustomMailDialog;

    /**
     * the last selected mail from the mail-box. sometimes even when some mail selected this variable will be null to indicate some action(for example
     * that the mail we send is to all the users)
     */
    public static GroupUser longClickSelected;
    public static boolean isReplyMail;

    private final Button btnSendMail;
    private EditText edtBodySendMail;

    /**
     * this EditText is passed to FirebaseMailSystem because in some mails(for example reply mails) we want the title to remain the same - so we have to update it in fms
     */
    private EditText edtTitleSendMail;

    /**
     * A long constructor that initializes all the variables, including the dialogs, what to do when positive/neutral/negative button clicked etc
     * @param activity MapsActivity
     * @param FBhelper Ref to FBhelper created in LVhelper
     * @param ADmanager Ref to ADmanager create in LVhelper
     */
    public UserOptionsSystem(Activity activity, FirebaseHelper FBhelper, AdapterManager ADmanager) {
        isReplyMail = false;
        this.ADmanager = ADmanager;
        this.FBhelper = FBhelper;

        chooseLeaderActionBuilder = new AlertDialog.Builder(activity);
        chooseLeaderActionBuilder.setMessage("What would you like to do?");
        chooseLeaderActionBuilder.setCancelable(true);

        chooseLeaderActionBuilder.setPositiveButton("Give Mine Leadership", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialogGiveLeadership.show();
            }
        });
        chooseLeaderActionBuilder.setNeutralButton("Send Mail", (dialog, which) -> {
            isReplyMail = false;
            edtTitleSendMail.setEnabled(true);
            createCustomMailDialog.show();
        });
        chooseLeaderActionBuilder.setNegativeButton("Kick", (dialog, which) -> FBhelper.fgs.DB_kickUser(longClickSelected));

        chooseParticipantActionBuilder = new AlertDialog.Builder(activity);
        chooseParticipantActionBuilder.setMessage("What you would like to do?");
        chooseParticipantActionBuilder.setCancelable(true);

        chooseParticipantActionBuilder.setNeutralButton("Send Mail", (dialog, which) -> {
            isReplyMail = false;
            edtTitleSendMail.setEnabled(true);
            edtTitleSendMail.setText("");
            edtBodySendMail.setText("");
            createCustomMailDialog.show();
        });



        sendGroupEmailActionBuilder = new AlertDialog.Builder(activity);
        sendGroupEmailActionBuilder.setMessage("What you would like to do?");
        sendGroupEmailActionBuilder.setCancelable(true);
        sendGroupEmailActionBuilder.setTitle("Choose Action");
        sendGroupEmailActionBuilder.setNeutralButton("Send Group Mail", (dialog, which) -> {
            longClickSelected = null;
            isReplyMail = false;
            edtTitleSendMail.setEnabled(true);
            createCustomMailDialog.show();
        });


        createCustomMailDialog = new Dialog(activity);
        createCustomMailDialog.setContentView(R.layout.create_custom_mail_layout);
        createCustomMailDialog.setTitle("my title");
        createCustomMailDialog.setCancelable(true);

        btnSendMail = createCustomMailDialog.findViewById(R.id.btnSendMail);
        edtTitleSendMail = createCustomMailDialog.findViewById(R.id.edtTitleSendMail);
        edtBodySendMail = createCustomMailDialog.findViewById(R.id.edtBodySendMail);
        btnSendMail.setOnClickListener(this);

        FBhelper.fgs.setEdtTitleSendMailRef(this.edtTitleSendMail);

        //get screen width, height
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        //set width and height of dialog relative to screen width and height
        createCustomMailDialog.getWindow().setLayout((int)(width / 1.3), (int)(height / 1.3));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Give Your Leadership");
        builder.setMessage("You Are about giving your leadership to someone else. You will become a participant. Are you sure?");
        builder.setCancelable(true);

        builder.setPositiveButton("Yes", (dialog, which) -> FBhelper.fgs.DB_giveLeadership(longClickSelected));
        builder.setNegativeButton("No", (dialog, which) -> {

        });
        alertDialogGiveLeadership = builder.create();
    }

    /**
     * When user click on 'Request' button(btnJoinLeaveGroup in uiHelper) we want to send mail to the group leader that I want to join the group, without ability to change title;
     * shows 'createCustomMailDialog' but not allows to change title(edtTitleSendMail.setEnabled(false)
     */
    public void requestJoinGroup() {
        longClickSelected = new GroupUser(ADmanager.lastSelectedGroup.leaderUid, "idc", "none");
        edtTitleSendMail.setText(FirebaseHelper.current.name + " requests to join group " + ADmanager.lastSelectedGroup.name);
        edtTitleSendMail.setEnabled(false);
        createCustomMailDialog.show();
    }


    /**
     * im the group leader that long-clicked on one of the participants, show me a dialog that says what can i do(kick, send mail, promote to leader)
     * @param gu the groupUser that I long-clicked on(from MapsActivity)
     */
    public void showLeaderOptionsDialog(GroupUser gu) {
        longClickSelected = gu;
        chooseLeaderActionBuilder.setTitle("Choose Action For " + gu.name);
        chooseLeaderActionBuilder.create().show();
    }

    /**
     * im a simple group participant that long-clicked on one of other participants, show me a dialog that says what can i do(only send mail)
     * @param gu the groupUser that I long-clicked on(from MapsActivity)
     */
    public void showParticipantOptionsDialog(GroupUser gu) {
        longClickSelected = gu;
        chooseParticipantActionBuilder.setTitle("Choose Action For " + gu.name);
        chooseParticipantActionBuilder.create().show();
    }

    /**
     * Im leader of the group and long-clicked on myself, show me dialog of what can i do(send mail to all group participants at once)
     */
    public void showLeaderGroupMailDialog() {
        sendGroupEmailActionBuilder.create().show();
    }

    /**
     * handles click on btnSendMail. Initializes the mail with data got from the sendMailDialog and passes it to FBhelper.fgs.fms to send it to firebase and the user
     * @param v the clicked view
     */
    @Override
    public void onClick(View v) {
        if(v == btnSendMail) {
            boolean problem = false;
            if(edtTitleSendMail.getText().toString().equals("")) {
                edtTitleSendMail.setError("Required field");
                problem = true;
            }
            if(edtBodySendMail.getText().toString().equals("")) {
                edtBodySendMail.setError("Required field");
                problem = true;
            }
            if(problem)
                return;
            String event = "";
            if(longClickSelected == null)
                event = MailAdapter.MAIL_EVENT_CUSTOM_GROUP_MAIL;
            else {
                if(isReplyMail)
                    event = MailAdapter.MAIL_EVENT_CUSTOM_REPLY;
                else if(edtTitleSendMail.isEnabled())
                    event = MailAdapter.MAIL_EVENT_CUSTOM_SINGLE;
                else
                    event = MailAdapter.MAIL_EVENT_GROUP_JOIN_REQUEST;
            }
            Mail mail = null;
            if(event.equals(MailAdapter.MAIL_EVENT_GROUP_JOIN_REQUEST))
                mail = new Mail(FirebaseHelper.current.uid, edtTitleSendMail.getText().toString(), FirebaseHelper.current.name, event, edtBodySendMail.getText().toString(), ADmanager.lastSelectedGroup.key);
            else
                mail = new Mail(FirebaseHelper.current.uid, edtTitleSendMail.getText().toString(), FirebaseHelper.current.name, event, edtBodySendMail.getText().toString());
            if(longClickSelected == null)
                FBhelper.fgs.fms.DB_sendMail(mail);
            else
                FBhelper.fgs.fms.DB_sendMail(longClickSelected.uid, mail);
            createCustomMailDialog.dismiss();
        }
    }
}
