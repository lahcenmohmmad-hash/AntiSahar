package com.antisahar.app.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.antisahar.app.R;
import com.antisahar.app.data.Prefs;
import com.antisahar.app.ui.LockedScreenActivity;
import com.antisahar.app.util.TimeUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * Two responsibilities:
 *
 *  1) While a lock window is active, detect when the user opens any package
 *     in the blocked list, and immediately launch our LockedScreenActivity
 *     on top to block them. We also send the user back to Home.
 *
 *  2) During a lock window, if the user tries to open the Settings screen
 *     that would let them disable our Device Admin or uninstall the app,
 *     we intercept the event and bounce them back to Home.
 */
public class LockAccessibilityService extends AccessibilityService {

    private static final long DEBOUNCE_MS = 400;
    private long lastActionAt = 0;

    private static final Set<String> SYSTEM_SETTINGS_PKGS = new HashSet<>();
    static {
        SYSTEM_SETTINGS_PKGS.add("com.android.settings");
        SYSTEM_SETTINGS_PKGS.add("com.google.android.packageinstaller");
        SYSTEM_SETTINGS_PKGS.add("com.android.packageinstaller");
        SYSTEM_SETTINGS_PKGS.add("com.miui.securitycenter");
        SYSTEM_SETTINGS_PKGS.add("com.samsung.android.packageinstaller");
    }

    private static final String[] BLOCKED_SETTINGS_KEYWORDS = new String[]{
            "uninstall", "إلغاء التثبيت", "حذف",
            "device admin", "مدير الجهاز",
            "force stop", "إيقاف", "ايقاف",
            "clear data", "مسح البيانات", "مسح بيانات",
            "accessibility", "إمكانية الوصول"
    };

    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.DEFAULT
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();

        // ignore ourselves
        if (pkg.equals(getPackageName())) return;

        long now = System.currentTimeMillis();
        if (now - lastActionAt < DEBOUNCE_MS) return;

        Prefs prefs = new Prefs(this);
        boolean locked = TimeUtil.isInLockWindow(prefs);

        // 1) Blocked apps logic
        if (locked) {
            Set<String> blocked = prefs.getBlockedApps();
            if (blocked.contains(pkg)) {
                lastActionAt = now;
                bounceToLockScreen(prefs);
                return;
            }
        }

        // 2) Self-protection in Settings during lock
        if (locked && SYSTEM_SETTINGS_PKGS.contains(pkg)) {
            CharSequence textCs = event.getText() != null && !event.getText().isEmpty()
                    ? event.getText().get(0) : null;
            String text = textCs != null ? textCs.toString().toLowerCase() : "";
            // simple heuristic: if the visible screen text mentions our app
            // OR mentions any dangerous keyword, send the user home.
            boolean mentionsUs = text.contains("anti-sahar")
                    || text.contains("antisahar")
                    || text.contains("مضاد السهر")
                    || text.contains(getPackageName());
            boolean dangerous = false;
            for (String kw : BLOCKED_SETTINGS_KEYWORDS) {
                if (text.contains(kw.toLowerCase())) { dangerous = true; break; }
            }
            if (mentionsUs && dangerous) {
                lastActionAt = now;
                Toast.makeText(this, R.string.cannot_uninstall_now, Toast.LENGTH_LONG).show();
                performGlobalAction(GLOBAL_ACTION_HOME);
            }
        }
    }

    private void bounceToLockScreen(Prefs prefs) {
        // First, jump to home so the underlying blocked app is not in the back stack.
        performGlobalAction(GLOBAL_ACTION_HOME);
        // Then open our full-screen lock overlay activity.
        main.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(LockAccessibilityService.this, LockedScreenActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                i.putExtra(LockedScreenActivity.EXTRA_LOCK_END, prefs.getLockEndEpoch());
                startActivity(i);
            }
        }, 120);
    }

    @Override
    public void onInterrupt() { /* no-op */ }

    /** Convenience: check whether our accessibility service is enabled. */
    public static boolean isEnabled(Context ctx) {
        String enabled = android.provider.Settings.Secure.getString(
                ctx.getContentResolver(),
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        String me = ctx.getPackageName() + "/" + LockAccessibilityService.class.getName();
        return enabled.contains(me);
    }
}
