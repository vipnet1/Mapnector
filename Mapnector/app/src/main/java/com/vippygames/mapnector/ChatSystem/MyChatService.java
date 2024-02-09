package com.vippygames.mapnector.ChatSystem;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.vippygames.mapnector.DBStorage.ChatMessage;
import com.vippygames.mapnector.LoginSystem.LoginActivity;
import com.vippygames.mapnector.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

/**
 * the aim of this service is to show welcome message after boot, or the last message in the chat we are listening to
 */
public class MyChatService extends Service {

    private final String CHANNEL_ID = "All Group Channel";
    private GenericTypeIndicator<List<ChatMessage>> genericTypeIndicator;
    private NotificationManager notificationManager;
    /**
     * the name of the group we are listening to
     */
    private String gName;
    /**
     * the key of the group we are listening to
     */
    public static String gKey="";
    /**
     * if true the pendingIntent should redirect as to LoginActivity, if false to the chat room;
     * false only in case we came from BootReceiver so we want to start app on notification click
     */
    private boolean pendingIntentToApp;

    /**
     * reference to the chat we listen in the firebase
     */
    private DatabaseReference currChatRef;
    /**
     * reference to the chat listener - in order to remove it later
     */
    private ValueEventListener chatListener;

    /**
     * initializes everything, checks intent extras if pendingIntent should be to app or not, and in case we aren't from BootReceiver binds listener to chat in firebase
     * that will show last message in notification bar
     * @param intent
     * @param flags
     * @param startId
     * @return returns always START_STICKY
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        pendingIntentToApp = false;
        gKey = intent.getStringExtra("gKey");
        this.gName = intent.getStringExtra("gName");
        if(gKey.equals("-") || gName.equals("-")) { //means after boot
            pendingIntentToApp = true;
            gKey = "";
            gName = "";
        }
        genericTypeIndicator = new GenericTypeIndicator<List<ChatMessage>>() {};
        notificationManager =(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        startForeground(1, buildNotification("Have a nice Day :)"));
        if(!gKey.equals("") && !gName.equals("") && !pendingIntentToApp) {
            chatListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.getValue() != null) {
                        List<ChatMessage> list = snapshot.getValue(genericTypeIndicator);
                        ChatMessage cm = list.get(list.size() - 1);
                        Notification notification = buildNotification(cm.senderName + ": " + cm.msg);
                        notificationManager.notify(1, notification);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            currChatRef = FirebaseDatabase.getInstance().getReference().child("Chats").child(gKey);
            currChatRef.addValueEventListener(chatListener);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        currChatRef.removeEventListener(chatListener);
        super.onDestroy();
    }

    /**
     * build a new notification with the text specified in the params
     * @param text the text that should appear in the notification built
     * @return
     */
    public Notification buildNotification(String text){

        int icon = R.drawable.ic_baseline_send_24;
        long when = System.currentTimeMillis();
        String ticker = "ticker";

        createNotificationChannel();
        PendingIntent pendingIntent = null;
        if(pendingIntentToApp) {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
        }
        else if(!gKey.equals("") && !gName.equals("")) {
            Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
            intent.putExtra("groupKey", gKey);
            intent.putExtra("groupName", gName);
            pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);

        return builder.setSmallIcon(icon).setTicker(ticker).setWhen(when).setAutoCancel(true).setContentIntent(pendingIntent)
                .setContentTitle(this.gName).setContentText(text).build();
    }

    /**
     * create the notification channel(if needed) required to show notifications from api oreo and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
