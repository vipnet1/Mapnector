package com.example.mapnector.ChatSystem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * class that extends BroadcastReceiver. Its aim is to listen for an event - if boot completed(no matter restart, cold boot or something else) and if
 * this event occurred it will start 'MyChatService' that will show a notification("have a nice day")
 */
public class BootReceiver extends BroadcastReceiver {
    /**
     * if boot completed create intent that redirects to MyChatService.class; and add sign to the intentExtras so it knows that we came from BootReceiver
     * @param context the context that calls the BootReceiver - usually will be main screen of your phone
     * @param intent the intent that redirected us to this part of code
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent newIntent = new Intent(context, MyChatService.class);
            newIntent.putExtra("gKey", "-");
            newIntent.putExtra("gName", "-");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(newIntent);
            }
            else {
                context.startService(newIntent);
            }
        }
    }
}
