package com.antisahar.app.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;

/**
 * Allows us to request the "uninstall protection" power via Device Admin.
 * Note: standard Device Admin can't directly block uninstall on modern Android,
 * but enabling it forces the user to disable admin first — combined with our
 * AccessibilityService which intercepts that screen during lock windows,
 * the user cannot uninstall during the sleep lock.
 */
public class AdminReceiver extends DeviceAdminReceiver {

    public static ComponentName getComponent(Context ctx) {
        return new ComponentName(ctx, AdminReceiver.class);
    }

    @Override
    public void onEnabled(Context context, android.content.Intent intent) {
        super.onEnabled(context, intent);
    }
}
