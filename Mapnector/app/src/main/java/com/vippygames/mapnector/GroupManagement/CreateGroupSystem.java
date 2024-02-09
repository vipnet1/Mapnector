package com.vippygames.mapnector.GroupManagement;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Point;
import android.view.Display;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.vippygames.mapnector.DBStorage.Group;
import com.vippygames.mapnector.FirebaseCommunication.FirebaseHelper;
import com.vippygames.mapnector.LVAdapters.GroupAdapter;
import com.vippygames.mapnector.MapsActivity;
import com.vippygames.mapnector.R;

/**
 * a small class that is used to help create a new group via dialog; made to split the work on LVHelper
 */
public class CreateGroupSystem {
    private final Dialog createGroupDialog;
    private final TextView tvCreateGroupTitle;
    private final EditText edCreateGroupName;
    private final EditText edCreateGroupDescription;
    private final EditText edCreateGroupMaxParticipants;

    /**
     * now its not only for creating group but also for updating group settings, just decided to keep the name
     */
    private final Button btnCreateGroup;
    private final Spinner spinner;

    public int dialogVal; //0-creating group, 1-editing group

    public Button getBtnCreateGroup() {return this.btnCreateGroup;}

    /**
     * Checks if all required fields fill in correctly in the dialog; if so create new group and return it to the listView that will pass it to the firebase
     * @return the new group that was created form the filled data; if something wrong(error, empty fields) returns null
     */
    public Group createGroup() {
        if(edCreateGroupName.getText().toString().isEmpty()) {
            edCreateGroupName.setError("Required field");
            return null;
        }
        if(edCreateGroupMaxParticipants.getText().toString().equals("")) {
            edCreateGroupMaxParticipants.setError("Required field");
            return null;
        }
        if(Integer.parseInt(edCreateGroupMaxParticipants.getText().toString()) <= 0) {
            edCreateGroupMaxParticipants.setError("Minimal Value: 1");
            return null;
        }

        return new Group(edCreateGroupName.getText().toString(), edCreateGroupDescription.getText().toString(), FirebaseHelper.current.uid,
                Integer.parseInt(edCreateGroupMaxParticipants.getText().toString()), spinner.getSelectedItem().toString());
    }

    public void showDialog() {
        this.dialogVal = 0;
        tvCreateGroupTitle.setText("Create Group");
        edCreateGroupName.setEnabled(true);
        edCreateGroupName.setText("");
        edCreateGroupDescription.setText("");
        edCreateGroupMaxParticipants.setEnabled(true);
        edCreateGroupMaxParticipants.setText("10");
        spinner.setSelection(0, true);
        btnCreateGroup.setText("Create Group");
        this.createGroupDialog.show();
    }
    public void disableDialog() {this.createGroupDialog.dismiss();}

    /**
     * get current String in the spinner
     * @return current text in state spinner
     */
    public String getStateText() {return spinner.getSelectedItem().toString();}

    /**
     * the text in description textView(when you click on group the yellow field at top left)
     * @returntext in description field of group
     */
    public String getDescriptionText() {return this.edCreateGroupDescription.getText().toString();}

    /**
     * show dialog to update group settings
     * @param g group to update settings
     */
    public void showUpdateDialog(Group g) {
        this.dialogVal = 1;
        tvCreateGroupTitle.setText("Update Group");
        edCreateGroupName.setEnabled(false);
        edCreateGroupName.setText(g.name);
        edCreateGroupDescription.setText(g.description);
        edCreateGroupMaxParticipants.setEnabled(false);
        edCreateGroupMaxParticipants.setText(String.valueOf(g.maxParticipants));
        btnCreateGroup.setText("Update");
        switch(g.state) {
            case GroupAdapter.GROUP_STATE_OPEN:
                spinner.setSelection(0, true);
                break;
            case GroupAdapter.GROUP_STATE_CLOSE:
                spinner.setSelection(1, true);
                break;
            case GroupAdapter.GROUP_STATE_INVITE:
                spinner.setSelection(2, true);
                break;

        }
        this.createGroupDialog.show();
    }

    public CreateGroupSystem(Activity activity) {
        createGroupDialog = new Dialog(activity);
        createGroupDialog.setContentView(R.layout.create_group_layout);
        createGroupDialog.setTitle("Create Group");
        createGroupDialog.setCancelable(true);

        tvCreateGroupTitle = createGroupDialog.findViewById(R.id.tvTitle);
        edCreateGroupName = createGroupDialog.findViewById(R.id.edGroupName);
        edCreateGroupDescription = createGroupDialog.findViewById(R.id.edGroupDescription);
        edCreateGroupMaxParticipants = createGroupDialog.findViewById(R.id.edMaxParticipants);
        btnCreateGroup = createGroupDialog.findViewById(R.id.btnCreateGroup);

        spinner = createGroupDialog.findViewById(R.id.spGroupState);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource((MapsActivity)activity,
                R.array.group_state_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        //get screen properties to display dialog
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        createGroupDialog.getWindow().setLayout((int)(width / 1.3), (int)(height / 1.3));
    }
}
