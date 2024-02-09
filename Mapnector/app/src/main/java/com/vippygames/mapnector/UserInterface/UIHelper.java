package com.vippygames.mapnector.UserInterface;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.slidingpanelayout.widget.SlidingPaneLayout;

import com.vippygames.mapnector.GroupManagement.LVHelper;
import com.vippygames.mapnector.MapsActivity;
import com.vippygames.mapnector.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * This class is holding most of the views(except of the create group ones and mail system) that are shown in MapsActivity on the main screen + left listView.
 * All its needed for is to make other classes smaller and try to separate logic from ui stuff so it will be easier to navigate in the code for me.
 */
public class UIHelper {

    /**
     * listView in MapsActivity that shows groups/users
     */
    public ListView lv;
    /**
     * listView in MapsActivity that shows mails(in right slidingPane layout)
     */
    public ListView lvMails;
    public FloatingActionButton floatingEye, floatingArrowBack, floatingPlus;
    /**
     * The layout where the listView in mapsActivtiy located; the left part of the screen where map isn't shown is this layout
     */
    public LinearLayout groupLayout;
    public TextView tvGroupDescrip; //group description
    /**
     * Btn that is clicked when we want to join,leave or request group; Changing button view by situation
     */
    public Button btnJoinLeaveGroup;
    public EditText edtSearch;
    /**
     * send location to firebase in general
     */
    public CheckBox chkSendLocation;
    /**
     * whether to allow this group to see my location(only when users shown on left listView)
     */
    public CheckBox chkAccessLocation;

    public Button btnOpenChat;

    /**
     * Specifies what button is btnJoinLeaveGroup now; It is the same button that used for joining, leaving and requesting to join groups
     */
    public enum JoinLeaveBtnType {
        BTN_JOIN, BTN_LEAVE, BTN_INVITE
    }

    /**
     * Here we initialize the views, the SlidingPaneLayout(of mail system) and bind onClickListeners of the views to the MapsActivity
     * @param activity The MapsActivtiy in order to findViewsById and setOnClickListeners point to that activity
     */
    public UIHelper(Activity activity) {
        SlidingPaneLayout slidingPaneLayout = activity.findViewById(R.id.SlidingPanel);
        int r = (int)(Math.random() * 200) + 50;
        int g = (int)(Math.random() * 200) + 50;
        int b = (int)(Math.random() * 200) + 50;
        slidingPaneLayout.setSliderFadeColor(Color.rgb(r, g, b));
        slidingPaneLayout.setParallaxDistance(200);
        slidingPaneLayout.setCoveredFadeColor(Color.BLUE);
        slidingPaneLayout.openPane();

        lv = activity.findViewById(R.id.lvGroups);
        lvMails = activity.findViewById(R.id.lvMail);
        floatingEye = activity.findViewById(R.id.floatingEye);
        floatingArrowBack = activity.findViewById(R.id.floatingArrowBack);
        floatingPlus = activity.findViewById(R.id.floatingPlus);
        groupLayout = activity.findViewById(R.id.groupslayout);
        tvGroupDescrip = activity.findViewById(R.id.tvGroupDescrip);
        btnJoinLeaveGroup = activity.findViewById(R.id.btnJoinLeaveGroup);
        edtSearch = activity.findViewById(R.id.edtSearch);
        chkAccessLocation = activity.findViewById(R.id.chkAccessLocation);
        chkSendLocation = activity.findViewById(R.id.chkSendLocation);
        btnOpenChat = activity.findViewById(R.id.btnOpenChat);


        floatingPlus.setOnClickListener((MapsActivity)activity);
        floatingEye.setOnClickListener((MapsActivity)activity);
        floatingArrowBack.setOnClickListener((MapsActivity)activity);
        lv.setOnItemClickListener((MapsActivity)activity);
        lv.setOnItemLongClickListener((MapsActivity)activity);
        btnJoinLeaveGroup.setOnClickListener((MapsActivity)activity);
        btnOpenChat.setOnClickListener((MapsActivity)activity);
    }

    /**
     * Handles what to do with the views when button EYE was clicked in MapsActivity; stuff like hide lestView, showother buttons, change button icon etc
     */
    public void clickFloatingEye() {
        if (groupLayout.getVisibility() == View.VISIBLE) { //if its visible
            groupLayout.setVisibility(View.GONE); //hide it
            floatingArrowBack.setVisibility(View.GONE); //hide also arrow
            floatingEye.setImageResource(R.drawable.ic_baseline_visibility_24); //change icon
        } else {
            groupLayout.setVisibility(View.VISIBLE); //if not visible
            if (LVHelper.state == LVHelper.LV_STATE.LV_STATE_USERS) { //if we are on users view
                floatingArrowBack.setVisibility(View.VISIBLE); //also show arrow to go back to groups view
            }
            floatingEye.setImageResource(R.drawable.ic_baseline_visibility_off_24); //change icon
        }
    }

    /**
     * What to do in case ARROW_BACK clicked in MapsActivity, hide some buttons, show others etc
     */
    public void clickFloatingArrowBack() {
        chkSendLocation.setVisibility(View.VISIBLE);
        chkAccessLocation.setVisibility(View.GONE);
        floatingPlus.setVisibility(View.VISIBLE);
        floatingArrowBack.setVisibility(View.GONE); //disable arrow
        tvGroupDescrip.setVisibility(View.GONE); //disable stuff describing our specific group
        btnJoinLeaveGroup.setVisibility(View.GONE); //hid join button if enabled
        edtSearch.setVisibility(View.VISIBLE); //make search layout visible again
        btnOpenChat.setVisibility(View.GONE);
    }

    /**
     * function to customize btnJoinLeaveGroup by param, set it as we wish to
     * @param type The type of the button we should set on btnJoinLeaveGroup
     */
    public void updateJoinLeaveBtn(JoinLeaveBtnType type) {
        btnJoinLeaveGroup.setVisibility(View.VISIBLE);
        if(type == JoinLeaveBtnType.BTN_JOIN) {
            btnJoinLeaveGroup.setBackgroundColor(Color.parseColor("#FF9800"));
            btnJoinLeaveGroup.setText("Join");
        }
        else if(type == JoinLeaveBtnType.BTN_LEAVE) {
            btnJoinLeaveGroup.setBackgroundColor(Color.RED);
            btnJoinLeaveGroup.setText("Leave");
        }
        else if(type == JoinLeaveBtnType.BTN_INVITE) {
            btnJoinLeaveGroup.setBackgroundColor(Color.parseColor("#0083FF"));
            btnJoinLeaveGroup.setText("Request");
        }
    }

    /**
     * What to do when we select a group from the lest listView - hide some buttons, show others etc.
     */
    public void groupSelected() {
        floatingPlus.setVisibility(View.GONE);
        tvGroupDescrip.setVisibility(View.VISIBLE); //show group description
        edtSearch.setVisibility(View.GONE); //remove option to search
        chkSendLocation.setVisibility(View.GONE);
        floatingArrowBack.setVisibility(View.VISIBLE); //add option to go back to groups view
    }


}
