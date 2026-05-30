package com.antisahar.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.antisahar.app.R;
import com.antisahar.app.data.Prefs;
import com.antisahar.app.util.TimeUtil;

import java.util.concurrent.TimeUnit;

/**
 * Runs roughly every 15 minutes and:
 *  1) Computes today's gradually-decreased bedtime.
 *  2) If we crossed bedtime, opens the lock window for `lockHours` hours.
 *  3) Posts/updates a status notification.
 *
 * The actual app blocking is done in real time by {@link LockAccessibilityService}.
 */
public class DailyScheduleWorker extends Worker {

    public static final String UNIQUE_NAME = "antisahar_daily";
    public static final String CH_ID = "antisahar_status";

    public DailyScheduleWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        Prefs prefs = new Prefs(ctx);
        if (!prefs.isPlanActive()) return Result.success();

        int bedtimeMin = TimeUtil.computeBedtimeForToday(prefs);
        long nowMs = System.currentTimeMillis();
        long bedtimeMs = TimeUtil.nextBedtimeMillis(bedtimeMin);

        // If "next bedtime" is more than 23h in the future, that means today's
        // bedtime already passed. Compute today's actual bedtime moment.
        long todayBedtimeMs = bedtimeMs - TimeUnit.DAYS.toMillis(1);
        if (todayBedtimeMs > nowMs - TimeUnit.HOURS.toMillis(12)
                && todayBedtimeMs <= nowMs) {
            // we are AT or just past today's bedtime → open lock window
            long lockMs = prefs.getLockHours() * 3600_000L;
            long end = todayBedtimeMs + lockMs;
            if (prefs.getLockEndEpoch() < end) {
                prefs.setLockEndEpoch(end);
            }
        }

        notify(ctx, prefs, bedtimeMin);
        return Result.success();
    }

    private void notify(Context ctx, Prefs prefs, int bedtimeMin) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CH_ID,
                    ctx.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }

        boolean locked = TimeUtil.isInLockWindow(prefs);
        String title = locked
                ? ctx.getString(R.string.notif_locked_title)
                : ctx.getString(R.string.notif_active_title);
        String text  = locked
                ? ctx.getString(R.string.notif_locked_text,
                    TimeUtil.fmtClockFromEpoch(prefs.getLockEndEpoch()))
                : ctx.getString(R.string.notif_active_text, TimeUtil.fmtHHmm(bedtimeMin));

        Notification n = new NotificationCompat.Builder(ctx, CH_ID)
                .setSmallIcon(R.drawable.ic_moon_small)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .build();
        nm.notify(1001, n);
    }

    public static void scheduleDaily(Context ctx) {
        Constraints c = new Constraints.Builder().build();
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                DailyScheduleWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(c)
                .build();
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, req);
    }
}
