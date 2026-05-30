package com.antisahar.app.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import com.antisahar.app.R;

/**
 * Opens the "auto-start manager" of common OEMs that aggressively kill
 * background services (Xiaomi/MIUI, Huawei/EMUI, Oppo, Vivo, Samsung,
 * Letv, Honor, Asus, ...). This is THE most important step to make sure
 * the sleep-guard service is not killed by the manufacturer's task killer.
 */
public class AutoStartHelper {

    /** Try to open the OEM auto-start settings page. Returns true if launched. */
    public static boolean openAutoStartSettings(Context ctx) {
        String manuf = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase();

        Intent[] candidates;

        if (manuf.contains("xiaomi") || manuf.contains("redmi") || manuf.contains("poco")) {
            candidates = new Intent[]{
                    intent("com.miui.securitycenter",
                           "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                    intent("com.miui.securitycenter",
                           "com.miui.appmanager.ApplicationsDetailsActivity"),
            };
        } else if (manuf.contains("huawei") || manuf.contains("honor")) {
            candidates = new Intent[]{
                    intent("com.huawei.systemmanager",
                           "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                    intent("com.huawei.systemmanager",
                           "com.huawei.systemmanager.optimize.process.ProtectActivity"),
                    intent("com.huawei.systemmanager",
                           "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
            };
        } else if (manuf.contains("oppo")) {
            candidates = new Intent[]{
                    intent("com.coloros.safecenter",
                           "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                    intent("com.coloros.safecenter",
                           "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                    intent("com.oppo.safe",
                           "com.oppo.safe.permission.startup.StartupAppListActivity"),
            };
        } else if (manuf.contains("vivo")) {
            candidates = new Intent[]{
                    intent("com.vivo.permissionmanager",
                           "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                    intent("com.iqoo.secure",
                           "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            };
        } else if (manuf.contains("samsung")) {
            candidates = new Intent[]{
                    intent("com.samsung.android.lool",
                           "com.samsung.android.sm.ui.battery.BatteryActivity"),
                    intent("com.samsung.android.sm_cn",
                           "com.samsung.android.sm.ui.battery.BatteryActivity"),
            };
        } else if (manuf.contains("letv")) {
            candidates = new Intent[]{
                    intent("com.letv.android.letvsafe",
                           "com.letv.android.letvsafe.AutobootManageActivity"),
            };
        } else if (manuf.contains("asus")) {
            candidates = new Intent[]{
                    intent("com.asus.mobilemanager",
                           "com.asus.mobilemanager.entry.FunctionActivity"),
            };
        } else if (manuf.contains("nokia")) {
            candidates = new Intent[]{
                    intent("com.evenwell.powersaving.g3",
                           "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"),
            };
        } else {
            candidates = new Intent[]{};
        }

        for (Intent i : candidates) {
            try {
                if (i.resolveActivity(ctx.getPackageManager()) != null) {
                    ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    Toast.makeText(ctx, R.string.autostart_toast, Toast.LENGTH_LONG).show();
                    return true;
                }
            } catch (Exception ignored) {}
        }
        // Fallback: open the app's own info page
        try {
            Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:" + ctx.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            Toast.makeText(ctx, R.string.autostart_fallback, Toast.LENGTH_LONG).show();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private static Intent intent(String pkg, String cls) {
        Intent i = new Intent();
        i.setComponent(new ComponentName(pkg, cls));
        return i;
    }

    /** Returns a human-readable name of the OEM if it's a known aggressive one. */
    public static String getAggressiveOEMName() {
        String m = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase();
        if (m.contains("xiaomi") || m.contains("redmi") || m.contains("poco")) return "Xiaomi/MIUI";
        if (m.contains("huawei") || m.contains("honor")) return "Huawei/EMUI";
        if (m.contains("oppo")) return "Oppo";
        if (m.contains("vivo")) return "Vivo";
        if (m.contains("samsung")) return "Samsung";
        if (m.contains("letv")) return "Letv";
        if (m.contains("asus")) return "Asus";
        if (m.contains("nokia")) return "Nokia";
        return null;
    }
}
