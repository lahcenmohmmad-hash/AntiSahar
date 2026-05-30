package com.antisahar.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.antisahar.app.R;
import com.antisahar.app.data.Prefs;
import com.antisahar.app.util.TimeUtil;

public class LockedScreenActivity extends AppCompatActivity {

    public static final String EXTRA_LOCK_END = "lock_end";

    private long lockEnd;
    private TextView tvCountdown, tvSub;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            if (now >= lockEnd) { finish(); return; }
            tvCountdown.setText(TimeUtil.fmtCountdown(lockEnd - now));
            ui.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        
        setContentView(R.layout.activity_locked);
        setFinishOnTouchOutside(false);

        tvCountdown = findViewById(R.id.tvCountdown);
        tvSub       = findViewById(R.id.tvLockedSub);

        lockEnd = getIntent().getLongExtra(EXTRA_LOCK_END, 0);
        if (lockEnd <= 0) {
            // fall back to prefs
            lockEnd = new Prefs(this).getLockEndEpoch();
        }
        if (lockEnd <= System.currentTimeMillis()) { finish(); return; }

        tvSub.setText(getString(R.string.locked_sub, TimeUtil.fmtClockFromEpoch(lockEnd)));
        ui.post(tick);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        long end = intent.getLongExtra(EXTRA_LOCK_END, 0);
        if (end > lockEnd) lockEnd = end;
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(tick);
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        // Do not allow back during a real lock window; just send to home.
        if (System.currentTimeMillis() < lockEnd) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HOME
                || keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            // can't truly block these on modern Android; just let them go.
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
