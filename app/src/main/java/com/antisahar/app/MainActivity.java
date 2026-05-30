package com.antisahar.app;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.antisahar.app.data.Prefs;
import com.antisahar.app.receiver.AdminReceiver;
import com.antisahar.app.service.DailyScheduleWorker;
import com.antisahar.app.service.ForegroundLockService;
import com.antisahar.app.service.LockAccessibilityService;
import com.antisahar.app.ui.AppsPickerActivity;
import com.antisahar.app.ui.SetupActivity;
import com.antisahar.app.util.AutoStartHelper;
import com.antisahar.app.util.TimeUtil;

public class MainActivity extends AppCompatActivity {

    private TextView tvTonight, tvTarget, tvProgress;
    private ProgressBar progressBar;
    private LinearLayout permList;

    private Prefs prefs;

    @Override
    protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        prefs = new Prefs(this);

        // First-run: force the user to enter their own times before they see the dashboard
        if (!prefs.isPlanActive()) {
            startActivity(new Intent(this, SetupActivity.class)
                    .putExtra(SetupActivity.EXTRA_FIRST_RUN, true));
        }

        setContentView(R.layout.activity_main);

        tvTonight   = findViewById(R.id.tvTonightBedtime);
        tvTarget    = findViewById(R.id.tvTarget);
        tvProgress  = findViewById(R.id.tvProgress);
        progressBar = findViewById(R.id.progressBar);
        permList    = findViewById(R.id.permList);

        findViewById(R.id.btnEditPlan).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SetupActivity.class));
            }
        });
        findViewById(R.id.btnPickApps).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AppsPickerActivity.class));
            }
        });
        findViewById(R.id.btnReset).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmReset(); }
        });

        DailyScheduleWorker.scheduleDaily(this);
        if (prefs.isPlanActive()) ForegroundLockService.start(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboard();
        refreshPermissions();
    }

    private void refreshDashboard() {
        int today = TimeUtil.computeBedtimeForToday(prefs);
        tvTonight.setText(TimeUtil.fmtHHmm(today));
        tvTarget.setText(TimeUtil.fmtHHmm(prefs.getTargetBedtime()));
        int p = TimeUtil.computeProgress(prefs);
        tvProgress.setText(p + "%");
        progressBar.setProgress(p);
    }

    // -------- permissions UI --------
    private void refreshPermissions() {
        permList.removeAllViews();
        addPermRow(getString(R.string.perm_device_admin), isDeviceAdminActive(), new Runnable() {
            @Override public void run() { requestDeviceAdmin(); }
        });
        addPermRow(getString(R.string.perm_accessibility), LockAccessibilityService.isEnabled(this), new Runnable() {
            @Override public void run() { openAccessibilitySettings(); }
        });
        addPermRow(getString(R.string.perm_usage), hasUsageStats(), new Runnable() {
            @Override public void run() { openUsageAccess(); }
        });
        addPermRow(getString(R.string.perm_overlay), Settings.canDrawOverlays(this), new Runnable() {
            @Override public void run() { requestOverlay(); }
        });
        addPermRow(getString(R.string.perm_battery), isIgnoringBattery(), new Runnable() {
            @Override public void run() { requestIgnoreBattery(); }
        });

        // The mighty Auto-Start row (always shown — we can't reliably detect this)
        String oem = AutoStartHelper.getAggressiveOEMName();
        String label = oem != null
                ? getString(R.string.perm_autostart_oem, oem)
                : getString(R.string.perm_autostart);
        addPermRowAlwaysAction(label, getString(R.string.open), new Runnable() {
            @Override public void run() { AutoStartHelper.openAutoStartSettings(MainActivity.this); }
        });
    }

    private void addPermRow(String name, boolean granted, final Runnable onGrant) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_permission, permList, false);
        TextView tv = row.findViewById(R.id.tvPermName);
        Button btn = row.findViewById(R.id.btnGrant);
        tv.setText(name);
        if (granted) {
            btn.setText(R.string.granted);
            btn.setEnabled(false);
            btn.setAlpha(0.6f);
        } else {
            btn.setText(R.string.grant);
            btn.setEnabled(true);
            btn.setAlpha(1f);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { onGrant.run(); }
            });
        }
        permList.addView(row);
    }

    private void addPermRowAlwaysAction(String name, String actionText, final Runnable onAction) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_permission, permList, false);
        TextView tv = row.findViewById(R.id.tvPermName);
        Button btn = row.findViewById(R.id.btnGrant);
        tv.setText(name);
        btn.setText(actionText);
        btn.setEnabled(true);
        btn.setAlpha(1f);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onAction.run(); }
        });
        permList.addView(row);
    }

    private boolean isDeviceAdminActive() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(AdminReceiver.getComponent(this));
    }

    private void requestDeviceAdmin() {
        ComponentName cn = AdminReceiver.getComponent(this);
        Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn)
                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        getString(R.string.admin_description));
        startActivity(i);
    }

    private void openAccessibilitySettings() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private boolean hasUsageStats() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) return false;
        int mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mode = appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    getPackageName());
        } else {
            mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    getPackageName());
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void openUsageAccess() {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    private void requestOverlay() {
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    private boolean isIgnoringBattery() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void requestIgnoreBattery() {
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    private void confirmReset() {
        if (TimeUtil.isInLockWindow(prefs)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.cannot_uninstall_now)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_reset_title)
                .setMessage(R.string.confirm_reset_msg)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    prefs.clearAll();
                    refreshDashboard();
                    refreshPermissions();
                    startActivity(new Intent(MainActivity.this, SetupActivity.class)
                            .putExtra(SetupActivity.EXTRA_FIRST_RUN, true));
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}
