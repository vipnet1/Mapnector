package com.vippygames.mapnector.LVAdapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vippygames.mapnector.DBStorage.Group;
import com.vippygames.mapnector.FirebaseCommunication.FirebaseHelper;
import com.vippygames.mapnector.R;

import java.util.List;

/**
 * Class to store Group.class objects in ListView
 */
public class GroupAdapter extends ArrayAdapter<Group> {

    private final Context context;
    private final List<Group> objects;

    public static final String GROUP_STATE_OPEN = "Open";
    public static final String GROUP_STATE_CLOSE = "Close";
    public static final String GROUP_STATE_INVITE = "Invite";

    public GroupAdapter(Context context, int resource, int textViewResourceId, List<Group> objects) {
        super(context, resource, textViewResourceId, objects);
        this.context = context;
        this.objects = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
        View view = inflater.inflate(R.layout.group_layout, parent, false);

        TextView name = (TextView)view.findViewById(R.id.tvName);
        TextView us = (TextView)view.findViewById(R.id.tvUsers);

        Group curr = objects.get(position);

        if(curr.leaderUid.equals(FirebaseHelper.current.uid)) {
            TextView tvYou = (TextView)view.findViewById(R.id.tvYourGroup);
            tvYou.setBackgroundColor(Color.parseColor("#73E6FF"));
        }

        name.setText(curr.name);
        us.setText(curr.participants + "/" + curr.maxParticipants);

        return view;
    }
}
