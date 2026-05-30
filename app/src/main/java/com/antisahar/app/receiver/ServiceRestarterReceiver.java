package com.antisahar.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.antisahar.app.data.Prefs;
import com.antisahar.app.service.ForegroundLockService;

/**
 * Whenever this receiver wakes up (via alarm, screen on, package replaced...),
 * it makes sure the ForegroundLockService is alive. This is the second line
 * of defense against aggressive battery savers killing our service.
 */
public class ServiceRestarterReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Prefs prefs = new Prefs(context);
        if (prefs.isPlanActive()) {
            ForegroundLockService.start(context);
        }
    }
}
