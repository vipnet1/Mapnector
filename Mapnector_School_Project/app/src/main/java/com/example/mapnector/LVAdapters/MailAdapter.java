package com.example.mapnector.LVAdapters;

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

import com.example.mapnector.DBStorage.Mail;
import com.example.mapnector.R;

import java.util.List;

/**
 * Class to store Mail.class objects in ListView; Contains final Strings that are translated to event field in the Mail class;
 * Sets background color of the item by the type of the mail(the Mail.event)
 */
public class MailAdapter extends ArrayAdapter<Mail> {
    private final Context context;
    private final List<Mail> objects;

    public static final String MAIL_EVENT_GOT_LEADERSHIP = "Got Leadership";
    public static final String MAIL_EVENT_KICK = "Kick";
    public static final String MAIL_EVENT_CUSTOM_SINGLE = "Custom(Single)";
    public static final String MAIL_EVENT_CUSTOM_REPLY = "Custom(Reply)";
    public static final String MAIL_EVENT_CUSTOM_GROUP_MAIL = "Custom(Group Mail)";
    public static final String MAIL_EVENT_GROUP_JOIN_REQUEST = "Join Request";
    public static final String MAIL_EVENT_GROUP_INVITE_ACCEPTED = "Request Accepted";

    public MailAdapter(Context context, int resource, int textViewResourceId, List<Mail> objects) {
        super(context, resource, textViewResourceId, objects);
        this.context = context;
        this.objects = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
        View view = inflater.inflate(R.layout.mail_layout, parent, false);

        TextView title = (TextView)view.findViewById(R.id.tvMailTitle);
        TextView from = (TextView)view.findViewById(R.id.tvMailFrom);
        TextView event = (TextView)view.findViewById(R.id.tvMailEvent);
        TextView body = (TextView)view.findViewById(R.id.tvMailBody);

        Mail curr = objects.get(position);

        title.setText(curr.title);
        from.setText(curr.from);
        event.setText(curr.event);
        body.setText(curr.body);

        switch(curr.event) {
            case MAIL_EVENT_GOT_LEADERSHIP:
                view.setBackgroundColor(Color.YELLOW);
                break;
            case MAIL_EVENT_KICK:
                view.setBackgroundColor(Color.parseColor("#e05b2f"));
                break;
            case MAIL_EVENT_CUSTOM_SINGLE:
                view.setBackgroundColor(Color.parseColor("#ffffff"));
                break;
            case MAIL_EVENT_CUSTOM_REPLY:
                view.setBackgroundColor(Color.parseColor("#F9E7FF"));
                break;
            case MAIL_EVENT_CUSTOM_GROUP_MAIL:
                view.setBackgroundColor(Color.parseColor("#00FF5B"));
                break;
            case MAIL_EVENT_GROUP_JOIN_REQUEST:
                view.setBackgroundColor(Color.parseColor("#FF00B8"));
                break;
            case MAIL_EVENT_GROUP_INVITE_ACCEPTED:
                view.setBackgroundColor(Color.parseColor("#00FFBC"));
                break;
            default:
                view.setBackgroundColor(Color.GRAY);
                break;
        }

        return view;
    }
}