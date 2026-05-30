package com.antisahar.app.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.antisahar.app.MainActivity;
import com.antisahar.app.R;
import com.antisahar.app.data.Prefs;
import com.antisahar.app.util.TimeUtil;

/**
 * STRONGEST anti-kill protection on Android.
 *
 * This is a sticky foreground service that:
 *  - Shows a permanent notification (so the OS cannot kill it silently)
 *  - Holds a partial WakeLock so the CPU keeps running during lock windows
 *  - Re-schedules itself with AlarmManager.setExactAndAllowWhileIdle() as a backup
 *  - Restarts itself on TIME_TICK (every minute) as another redundancy
 *  - Returns START_STICKY so Android relaunches it after low-memory kills
 *  - Reschedules itself onTaskRemoved (when user swipes app from recents)
 */
public class ForegroundLockService extends Service {

    public static final String CH_ID = "antisahar_persistent";
    public static final int NOTIF_ID = 4242;
    public static final String ACTION_TICK = "com.antisahar.app.TICK";

    private PowerManager.WakeLock wakeLock;
    private final Handler main = new Handler(Looper.getMainLooper());

    private final BroadcastReceiver timeTickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tick();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIF_ID, buildNotification(getString(R.string.notif_active_title),
                getString(R.string.notif_persistent_text)));

        // Acquire a partial wake lock so the CPU stays responsive enough
        // to detect blocked app launches at night.
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "AntiSahar::SleepGuard");
            try { wakeLock.acquire(); } catch (Exception ignored) {}
        }

        // Listen for every-minute TIME_TICK from the system
        IntentFilter f = new IntentFilter(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        f.addAction(Intent.ACTION_SCREEN_ON);
        try { registerReceiver(timeTickReceiver, f); } catch (Exception ignored) {}

        // Also schedule a backup alarm every minute
        scheduleSelfAlarm();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        tick();
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // The user swiped us from recents — restart ourselves immediately.
        Intent restartIntent = new Intent(getApplicationContext(), ForegroundLockService.class);
        restartIntent.setPackage(getPackageName());

        PendingIntent restartPI = PendingIntent.getService(
                this, 1, restartIntent,
                PendingIntent.FLAG_ONE_SHOT
                        | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            ? PendingIntent.FLAG_IMMUTABLE : 0));
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, restartPI);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        try { unregisterReceiver(timeTickReceiver); } catch (Exception ignored) {}
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (Exception ignored) {}
        }
        // If we're being destroyed but the plan is active, ask the system to restart us
        Prefs prefs = new Prefs(this);
        if (prefs.isPlanActive()) {
            Intent broadcastIntent = new Intent(this, ServiceRestarterReceiver.class);
            sendBroadcast(broadcastIntent);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ------------------------------------------------------------------
    private void tick() {
        Prefs prefs = new Prefs(this);
        if (!prefs.isPlanActive()) {
            stopForeground(true);
            stopSelf();
            return;
        }

        int bedtimeMin = TimeUtil.computeBedtimeForToday(prefs);
        long now = System.currentTimeMillis();
        long todayBedtime = TimeUtil.todayBedtimeMillis(bedtimeMin);

        // Just passed bedtime → open the lock window
        if (now >= todayBedtime && now - todayBedtime < 60_000) {
            long end = todayBedtime + prefs.getLockHours() * 3600_000L;
            if (prefs.getLockEndEpoch() < end) {
                prefs.setLockEndEpoch(end);
            }
        }

        // Update notification
        boolean locked = TimeUtil.isInLockWindow(prefs);
        String title = locked
                ? getString(R.string.notif_locked_title)
                : getString(R.string.notif_active_title);
        String text  = locked
                ? getString(R.string.notif_locked_text,
                    TimeUtil.fmtClockFromEpoch(prefs.getLockEndEpoch()))
                : getString(R.string.notif_active_text, TimeUtil.fmtHHmm(bedtimeMin));

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(title, text));
    }

    private Notification buildNotification(String title, String text) {
        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            ? PendingIntent.FLAG_IMMUTABLE : 0));

        return new NotificationCompat.Builder(this, CH_ID)
                .setSmallIcon(R.drawable.ic_moon_small)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(pi)
                .setShowWhen(false)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel ch = new NotificationChannel(
                    CH_ID,
                    getString(R.string.notif_persistent_channel),
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription(getString(R.string.notif_persistent_desc));
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
    }

    private void scheduleSelfAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(this, ServiceRestarterReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 2, i,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            ? PendingIntent.FLAG_IMMUTABLE : 0));
        long triggerAt = System.currentTimeMillis() + 60_000;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException se) {
            // SCHEDULE_EXACT_ALARM not granted on Android 12+ — fall back to inexact
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    /** Start the service in a way that works on all Android versions. */
    public static void start(Context ctx) {
        Intent i = new Intent(ctx, ForegroundLockService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i);
            } else {
                ctx.startService(i);
            }
        } catch (Exception ignored) {}
    }
}
