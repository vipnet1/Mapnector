package com.example.mapnector.ChatSystem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import com.example.mapnector.DBStorage.ChatMessage;
import com.example.mapnector.LVAdapters.MessageAdapter;
import com.example.mapnector.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a chat room. It has listView for showing the messages of others and an editText at the bottom so I can send messages too.
 * it has menu with option to 'Listen' to current room so even when app is closed we can see the last message in notifications(via the service)
 */
public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    private ArrayList<ChatMessage> messages;
    private MessageAdapter messageAdapter;

    private FloatingActionButton btnSendMessage;
    private EditText edtMsg;
    /**
     * The key of the group that we are in it's room
     */
    private String gKey;
    /**
     * The name of the group that we are in it's room
     */
    private String gName;

    private ListView lvChat;

    /**
     * true if user wants he's name to not be displayed during conversation
     */
    private boolean isAnonymous;
    /**
     * the new name instead of the user's one in case isAnonymous true will be 'anonymous' + the indentificator
     */
    private int anonymousIdentificator;

    /**
     * required to get list of elements from firebase(in out case list of messages)
     */
    private GenericTypeIndicator<List<ChatMessage>> genericTypeIndicator;

    /**
     * reference to the chat we listen in the firebase
     */
    private DatabaseReference currChatRef;
    /**
     * reference to the chat listener - in order to remove it later
     */
    private ValueEventListener chatListener;

    /**
     * if we are already listening to this chat change the menu item action_listen to IGNORE and not LISTEN
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        if(gKey.equals(MyChatService.gKey))
            menu.findItem(R.id.action_listen).setTitle("IGNORE");
        return true;
    }

    /**
     * if we clicked on item with text "LISTEN" so start new MyChatService; otherwise disable MyChatService because we want to stop listening to notifications from that group;
     * if we clicked on the 'privacy' section we will enable/disable showing my name during chatting with others
     * @param item the selected menu item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        int id=item.getItemId();
        if(id == R.id.action_listen) {
            if(item.getTitle().toString().equals("LISTEN")) {
                Intent intent = new Intent(this, MyChatService.class);
                intent.putExtra("gKey", gKey);
                intent.putExtra("gName", gName);
                item.setTitle("IGNORE");
                startService(intent);
            }
            else if(item.getTitle().toString().equals("IGNORE")) {
                Intent intent = new Intent(this, MyChatService.class);
                item.setTitle("LISTEN");
                MyChatService.gKey = "";
                stopService(intent);
            }
        }
        else if(id == R.id.action_anonymous) {
            if(item.getTitle().toString().equals("Privacy: normal")) {
                item.setTitle("Privacy: anonymous");
            }
            else if(item.getTitle().toString().equals("Privacy: anonymous")) {
                item.setTitle("Privacy: normal");
            }
            isAnonymous = !isAnonymous;
        }
        return true;
    }

    /**
     * finish the activity onStop; made to avoid some weird bugs, for example that you can open multiple intents via notification
     */
    @Override
    protected void onStop() {
        super.onStop();
        currChatRef.removeEventListener(chatListener);
        finish();
    }

    /**
     * Simply initialize the variables; get the current groups name and key from intent's extras
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        this.isAnonymous = false;
        this.anonymousIdentificator = (int)(Math.random() * 999999);

        genericTypeIndicator = new GenericTypeIndicator<List<ChatMessage>>() {};

        gName = getIntent().getStringExtra("groupName");
        gKey = getIntent().getStringExtra("groupKey");

        if(gKey == null)
            finish();

        this.setTitle(gName);

        messages = new ArrayList<ChatMessage>();
        messageAdapter = new MessageAdapter(this, 0, 0, messages);
        lvChat = findViewById(R.id.lvChat);
        lvChat.setAdapter(messageAdapter);

        currChatRef = FirebaseDatabase.getInstance().getReference().child("Chats").child(gKey);
        chatListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.getValue() != null) {
                    List<ChatMessage> list = snapshot.getValue(genericTypeIndicator);
                    messageAdapter.clear();
                    messageAdapter.addAll(list);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        currChatRef.addValueEventListener(chatListener);

        this.edtMsg = findViewById(R.id.edtSendChatMsg);
        this.btnSendMessage = findViewById(R.id.btnSendChatMsg);
        this.btnSendMessage.setOnClickListener(this);
    }

    /**
     * listens to: btnSendMessage; if it clicked we check for problems(empty field), if field not empty send it to firebase(now the listener located here and not in FBhelper
     * because it may not be initialized if we closed app and came to activity from notification)
     */
    @Override
    public void onClick(View v) {
        if(v==btnSendMessage) {
            if(edtMsg.getText().toString().equals(""))
                return;
            String msg = edtMsg.getText().toString();
            edtMsg.setText("");
            FirebaseDatabase.getInstance().getReference().child("Chats").child(gKey).runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    List<ChatMessage> list;
                    ChatMessage mess = null;
                    if(isAnonymous)
                        mess = new ChatMessage(msg, anonymousIdentificator);
                    else
                        mess = new ChatMessage(msg);
                    if(currentData.getValue() != null) {
                        list = currentData.getValue(genericTypeIndicator);
                        list.add(mess);
                        currentData.setValue(list);
                    }
                    else {
                        list = new ArrayList<ChatMessage>();
                        list.add(mess);
                        currentData.setValue(list);
                    }
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                    lvChat.smoothScrollToPosition(messages.size() - 1);
                }
            });
        }
    }
}