package com.much.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Opens the MUCH Launcher as soon as the Fire TV Stick finishes starting up. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } catch (Exception ignored) {
        }
    }
}
