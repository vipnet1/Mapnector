package com.vippygames.mapnector;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class ExternalAppUtils {
    private final Context context;

    public ExternalAppUtils(Context context) {
        this.context = context;
    }

    public void tryOpenUri(String url, String failMessage) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, failMessage, Toast.LENGTH_SHORT).show();
        }
    }
}
