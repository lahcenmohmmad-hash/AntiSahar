package com.antisahar.app.ui;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.antisahar.app.R;
import com.antisahar.app.data.Prefs;
import com.antisahar.app.service.DailyScheduleWorker;
import com.antisahar.app.service.ForegroundLockService;
import com.antisahar.app.util.TimeUtil;

public class SetupActivity extends AppCompatActivity {

    public static final String EXTRA_FIRST_RUN = "first_run";
    private static final int UNSET = -1;

    private TextView tvCurrent, tvTarget, tvStep, tvLock, tvEstimate, tvTitle;
    private SeekBar seekStep, seekLock;

    /** -1 means "the user hasn't picked yet" (only for first run). */
    private int currentMin, targetMin, stepMin, lockHours;
    private boolean firstRun;
    private Prefs prefs;

    @Override
    protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_setup);

        prefs = new Prefs(this);
        firstRun = getIntent().getBooleanExtra(EXTRA_FIRST_RUN, false) || !prefs.isPlanActive();

        tvTitle    = findViewById(R.id.tvSetupTitle);
        tvCurrent  = findViewById(R.id.tvCurrent);
        tvTarget   = findViewById(R.id.tvTarget);
        tvStep     = findViewById(R.id.tvStepValue);
        tvLock     = findViewById(R.id.tvLockValue);
        tvEstimate = findViewById(R.id.tvEstimate);
        seekStep   = findViewById(R.id.seekStep);
        seekLock   = findViewById(R.id.seekLock);

        if (firstRun) {
            // No defaults — the user must consciously pick their own values
            currentMin = UNSET;
            targetMin  = UNSET;
            stepMin    = Prefs.DEFAULT_STEP_MINUTES;
            lockHours  = 2;
            tvTitle.setText(R.string.first_run_title);
            tvCurrent.setText(R.string.tap_to_pick);
            tvTarget.setText(R.string.tap_to_pick);
        } else {
            currentMin = prefs.getCurrentBedtime();
            targetMin  = prefs.getTargetBedtime();
            stepMin    = prefs.getStepMinutes();
            lockHours  = prefs.getLockHours();
            tvTitle.setText(R.string.setup_title);
            tvCurrent.setText(TimeUtil.fmtHHmm(currentMin));
            tvTarget.setText(TimeUtil.fmtHHmm(targetMin));
        }

        seekStep.setMax(Prefs.MAX_STEP_MINUTES - Prefs.MIN_STEP_MINUTES);
        seekStep.setProgress(clampStep(stepMin) - Prefs.MIN_STEP_MINUTES);
        seekLock.setMax(11);
        seekLock.setProgress(Math.max(0, Math.min(11, lockHours - 1)));

        updateStepLabel(); updateLockLabel(); updateEstimate();

        findViewById(R.id.btnPickCurrent).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickTime(true); }
        });
        findViewById(R.id.btnPickTarget).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickTime(false); }
        });
        // tap on the time text itself also opens the picker
        tvCurrent.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickTime(true); }
        });
        tvTarget.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickTime(false); }
        });

        seekStep.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                stepMin = p + Prefs.MIN_STEP_MINUTES;
                updateStepLabel();
                updateEstimate();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        seekLock.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                lockHours = p + 1; updateLockLabel();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        });
    }

    private int clampStep(int v) {
        if (v < Prefs.MIN_STEP_MINUTES) return Prefs.MIN_STEP_MINUTES;
        if (v > Prefs.MAX_STEP_MINUTES) return Prefs.MAX_STEP_MINUTES;
        return v;
    }

    private void pickTime(final boolean isCurrent) {
        int initVal = isCurrent ? currentMin : targetMin;
        if (initVal == UNSET) initVal = isCurrent ? (2 * 60) : (23 * 60); // sensible starting point
        final int init = initVal;
        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override public void onTimeSet(android.widget.TimePicker tp, int hour, int min) {
                int v = hour * 60 + min;
                if (isCurrent) { currentMin = v; tvCurrent.setText(TimeUtil.fmtHHmm(v)); }
                else           { targetMin  = v; tvTarget.setText(TimeUtil.fmtHHmm(v));  }
                updateEstimate();
            }
        }, init / 60, init % 60, true).show();
    }

    private void updateStepLabel() {
        tvStep.setText(stepMin + " " + getString(R.string.minutes_short));
    }
    private void updateLockLabel() {
        tvLock.setText(lockHours + " " + getString(R.string.hours_short));
    }

    private void updateEstimate() {
        if (currentMin == UNSET || targetMin == UNSET) {
            tvEstimate.setText(R.string.estimate_pick_first);
            return;
        }
        int curLin = currentMin < 12 * 60 ? currentMin + 24 * 60 : currentMin;
        int tgtLin = targetMin  < 12 * 60 ? targetMin  + 24 * 60 : targetMin;
        int diff = curLin - tgtLin;
        if (diff <= 0) {
            tvEstimate.setText(getString(R.string.estimate_invalid));
            return;
        }
        int days = (int) Math.ceil((double) diff / Math.max(1, stepMin));
        tvEstimate.setText(getString(R.string.estimate_days, days));
    }

    private void save() {
        if (currentMin == UNSET) {
            Toast.makeText(this, R.string.error_pick_current, Toast.LENGTH_LONG).show(); return;
        }
        if (targetMin == UNSET) {
            Toast.makeText(this, R.string.error_pick_target, Toast.LENGTH_LONG).show(); return;
        }
        if (stepMin < Prefs.MIN_STEP_MINUTES || stepMin > Prefs.MAX_STEP_MINUTES) {
            Toast.makeText(this, R.string.error_invalid_step, Toast.LENGTH_LONG).show(); return;
        }
        if (lockHours < 1 || lockHours > 12) {
            Toast.makeText(this, R.string.error_invalid_lock, Toast.LENGTH_LONG).show(); return;
        }
        int curLin = currentMin < 12 * 60 ? currentMin + 24 * 60 : currentMin;
        int tgtLin = targetMin  < 12 * 60 ? targetMin  + 24 * 60 : targetMin;
        if (tgtLin >= curLin) {
            Toast.makeText(this, R.string.error_target_after_current, Toast.LENGTH_LONG).show(); return;
        }

        prefs.setPlan(currentMin, targetMin, stepMin, lockHours, TimeUtil.todayEpochDay());
        DailyScheduleWorker.scheduleDaily(this);
        ForegroundLockService.start(this);
        finish();
    }
}
