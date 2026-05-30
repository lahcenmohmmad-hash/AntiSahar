package com.antisahar.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.antisahar.app.data.Prefs;
import com.antisahar.app.service.DailyScheduleWorker;
import com.antisahar.app.service.ForegroundLockService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String act = intent.getAction();
        if (act == null) return;
        if (act.equals(Intent.ACTION_BOOT_COMPLETED)
                || act.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)
                || act.equals(Intent.ACTION_MY_PACKAGE_REPLACED)
                || act.equals(Intent.ACTION_PACKAGE_REPLACED)
                || act.equals("android.intent.action.QUICKBOOT_POWERON")) {
            DailyScheduleWorker.scheduleDaily(context);
            Prefs prefs = new Prefs(context);
            if (prefs.isPlanActive()) {
                ForegroundLockService.start(context);
            }
        }
    }
}
