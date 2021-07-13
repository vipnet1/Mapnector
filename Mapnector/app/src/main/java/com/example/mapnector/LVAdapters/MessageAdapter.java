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

import com.example.mapnector.DBStorage.ChatMessage;
import com.example.mapnector.R;

import java.util.HashMap;
import java.util.List;

/**
 * Class to store ChatMessage.class objects in ListView
 */

public class MessageAdapter extends ArrayAdapter<ChatMessage> {
    private final Context context;
    private final List<ChatMessage> objects;

    /**
     * saves with what color to draw each name in the listView of the chat
     */
    private HashMap<String, MyColor> pallete;

    /**
     * a class that represents color - has 3 integers r,g,b and simple constructor
     */
    public static class MyColor {
        public int r, g, b;

        public MyColor(int r, int g, int b) {
            this.r = r; this.g = g; this.b = b;
        }
    }

    public MessageAdapter(Context context, int resource, int textViewResourceId, List<ChatMessage> objects) {
        super(context, resource, textViewResourceId, objects);
        this.pallete = new HashMap<String, MyColor>();
        this.context = context;
        this.objects = objects;
    }

    /**
     * generates random number between values specified
     * @return the random number
     */
    private int GenInt30_210() {
        return (int)(Math.random() * 180 + 30);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
        View view = inflater.inflate(R.layout.message_layout, parent, false);

        TextView senderName = (TextView)view.findViewById(R.id.tvChatSenderName);
        TextView msg = (TextView)view.findViewById(R.id.tvChatMsg);

        ChatMessage curr = objects.get(position);

        if(!pallete.containsKey(curr.senderName)) {
            MyColor clr = new MyColor(GenInt30_210(), GenInt30_210(), GenInt30_210());
            pallete.put(curr.senderName, clr);
        }
        MyColor clr = pallete.get(curr.senderName);
        senderName.setTextColor(Color.rgb(clr.r, clr.g, clr.b));

        senderName.setText(curr.senderName + ": ");
        msg.setText(curr.msg);

        return view;
    }
}
