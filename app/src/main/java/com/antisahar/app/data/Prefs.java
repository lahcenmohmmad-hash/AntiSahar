package com.antisahar.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Thin wrapper over SharedPreferences holding the entire app state.
 * All times are stored as "minutes since midnight" (0..1439).
 */
public class Prefs {

    private static final String FILE = "antisahar_prefs";

    private static final String K_CURRENT_BEDTIME = "current_bedtime";   // user's starting bedtime
    private static final String K_TARGET_BEDTIME  = "target_bedtime";    // user's goal bedtime
    private static final String K_STEP_MINUTES    = "step_minutes";      // minutes to subtract each day
    private static final String K_LOCK_HOURS      = "lock_hours";        // duration of lock window after bedtime
    private static final String K_PLAN_START_DAY  = "plan_start_day";    // day-index (epoch days) when plan started
    private static final String K_BLOCKED_APPS    = "blocked_apps";      // Set<String> package names
    private static final String K_PLAN_ACTIVE     = "plan_active";
    private static final String K_LOCK_END_EPOCH  = "lock_end_epoch";    // millis until lock window ends today

    /** Default daily step is 2 minutes — small enough to be imperceptible. */
    public static final int DEFAULT_STEP_MINUTES = 2;
    /** Allowed range for the daily step. Keep it small so the change is "magical". */
    public static final int MIN_STEP_MINUTES = 1;
    public static final int MAX_STEP_MINUTES = 15;

    private final SharedPreferences sp;

    public Prefs(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // -------- bedtime values (minutes since midnight) --------
    public int getCurrentBedtime() { return sp.getInt(K_CURRENT_BEDTIME, 3 * 60 + 30); /* 03:30 */ }
    public int getTargetBedtime()  { return sp.getInt(K_TARGET_BEDTIME, 23 * 60);       /* 23:00 */ }
    public int getStepMinutes()    { return sp.getInt(K_STEP_MINUTES, DEFAULT_STEP_MINUTES); }
    public int getLockHours()      { return sp.getInt(K_LOCK_HOURS, 2); }
    public long getPlanStartDay()  { return sp.getLong(K_PLAN_START_DAY, 0L); }
    public boolean isPlanActive()  { return sp.getBoolean(K_PLAN_ACTIVE, false); }
    public long getLockEndEpoch()  { return sp.getLong(K_LOCK_END_EPOCH, 0L); }

    public Set<String> getBlockedApps() {
        Set<String> s = sp.getStringSet(K_BLOCKED_APPS, Collections.<String>emptySet());
        return new HashSet<>(s != null ? s : Collections.<String>emptySet());
    }

    public void setPlan(int currentBedtime, int targetBedtime, int step, int lockHours, long planStartDay) {
        sp.edit()
                .putInt(K_CURRENT_BEDTIME, currentBedtime)
                .putInt(K_TARGET_BEDTIME, targetBedtime)
                .putInt(K_STEP_MINUTES, step)
                .putInt(K_LOCK_HOURS, lockHours)
                .putLong(K_PLAN_START_DAY, planStartDay)
                .putBoolean(K_PLAN_ACTIVE, true)
                .apply();
    }

    public void setBlockedApps(Set<String> apps) {
        sp.edit().putStringSet(K_BLOCKED_APPS, new HashSet<>(apps)).apply();
    }

    public void setLockEndEpoch(long epochMs) {
        sp.edit().putLong(K_LOCK_END_EPOCH, epochMs).apply();
    }

    public void clearAll() {
        sp.edit().clear().apply();
    }
}
