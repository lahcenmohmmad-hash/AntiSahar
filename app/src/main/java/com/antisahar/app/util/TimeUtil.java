package com.antisahar.app.util;

import com.antisahar.app.data.Prefs;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Pure helpers around the "minutes-since-midnight" representation
 * and the daily-decreasing-bedtime logic.
 */
public class TimeUtil {

    public static String fmtHHmm(int minutesSinceMidnight) {
        int m = ((minutesSinceMidnight % (24 * 60)) + (24 * 60)) % (24 * 60);
        int h = m / 60;
        int mm = m % 60;
        return String.format(Locale.US, "%02d:%02d", h, mm);
    }

    /** Current local day-index (epoch days). */
    public static long todayEpochDay() {
        long offset = TimeZone.getDefault().getOffset(System.currentTimeMillis());
        return (System.currentTimeMillis() + offset) / (24L * 60L * 60L * 1000L);
    }

    /**
     * The "smart" bedtime for today: gradually decreases from current
     * toward target by `step` minutes per elapsed day, stopping at target.
     */
    public static int computeBedtimeForToday(Prefs prefs) {
        int current = prefs.getCurrentBedtime();
        int target  = prefs.getTargetBedtime();
        int step    = Math.max(1, prefs.getStepMinutes());
        long start  = prefs.getPlanStartDay();
        long today  = todayEpochDay();

        if (start <= 0) start = today;
        long days = Math.max(0, today - start);

        // bedtimes between 00:00-11:59 are treated as +24h so they
        // come "after" evening times on a continuous line.
        int currentLin = current < 12 * 60 ? current + 24 * 60 : current;
        int targetLin  = target  < 12 * 60 ? target  + 24 * 60 : target;

        if (targetLin >= currentLin) return current;

        long delta = days * step;
        int computedLin = (int) Math.max(targetLin, currentLin - delta);
        return computedLin % (24 * 60);
    }

    /** Next bedtime moment (today if not yet, else tomorrow). */
    public static long nextBedtimeMillis(int bedtimeMinutes) {
        Calendar now = Calendar.getInstance();
        Calendar c = (Calendar) now.clone();
        c.set(Calendar.HOUR_OF_DAY, bedtimeMinutes / 60);
        c.set(Calendar.MINUTE, bedtimeMinutes % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (!c.after(now)) c.add(Calendar.DAY_OF_YEAR, 1);
        return c.getTimeInMillis();
    }

    /** Today's bedtime moment in millis (may be in the past). */
    public static long todayBedtimeMillis(int bedtimeMinutes) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, bedtimeMinutes / 60);
        c.set(Calendar.MINUTE, bedtimeMinutes % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        // If the bedtime is "after midnight" (i.e. early morning hours like 01:30),
        // treat it as the SAME day's late-night moment (already past noon mark)
        // not as the literal AM moment of today. We anchor it to "the next bedtime
        // moment minus 24h" if that gives a still-future-relative-to-noon timing.
        return c.getTimeInMillis();
    }

    public static boolean isInLockWindow(Prefs prefs) {
        long end = prefs.getLockEndEpoch();
        return end > 0 && System.currentTimeMillis() < end;
    }

    public static String fmtClockFromEpoch(long epochMs) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(epochMs);
        return String.format(Locale.US, "%02d:%02d",
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    public static String fmtCountdown(long millis) {
        if (millis < 0) millis = 0;
        long s = millis / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, sec);
    }

    public static int computeProgress(Prefs prefs) {
        int current = prefs.getCurrentBedtime();
        int target  = prefs.getTargetBedtime();
        int today   = computeBedtimeForToday(prefs);

        int currentLin = current < 12 * 60 ? current + 24 * 60 : current;
        int targetLin  = target  < 12 * 60 ? target  + 24 * 60 : target;
        int todayLin   = today   < 12 * 60 ? today   + 24 * 60 : today;

        int total = currentLin - targetLin;
        if (total <= 0) return 100;
        int done = currentLin - todayLin;
        int pct = (int) Math.round(100.0 * done / total);
        if (pct < 0) pct = 0; if (pct > 100) pct = 100;
        return pct;
    }
}
