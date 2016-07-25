package com.louiswins.joggingtimer;

import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimerActivity extends AppCompatActivity {
    private static final String WORKOUT_IDX = "com.louiswins.joggingtimer.workoutIdx";

    private PausableCountDownTimer timer;
    private WorkoutSet currentWorkoutSet;
    private int workoutIdx;
    private int segmentIdx;

    private TextView title;
    private TextView description;
    private Button back;
    private Button forth;
    private Button startPause;
    private TextView countingUp;
    private TextView countingDown;
    private ProgressBar progressBar;
    private TextView currentAction;
    private TextView countingUpTotal;
    private TextView countingDownTotal;
    private ProgressBar totalBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread thread, Throwable e)
            {
                startActivity(new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_EMAIL, new String[] {"logs@louiswins.com"})
                    .putExtra(Intent.EXTRA_SUBJECT, "Jogging Timer crash log")
                    .putExtra(Intent.EXTRA_TEXT, Log.getStackTraceString(e)));
                prev.uncaughtException(thread, e);
            }
        });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        // TODO: make this a preference
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        title = (TextView) findViewById(R.id.title);
        description = (TextView) findViewById(R.id.description);
        back = (Button) findViewById(R.id.back);
        forth = (Button) findViewById(R.id.forth);
        startPause = (Button) findViewById(R.id.startPause);
        countingUp = (TextView) findViewById(R.id.counting_up);
        countingDown = (TextView) findViewById(R.id.counting_down);
        progressBar = (ProgressBar) findViewById(R.id.timer_bar);
        currentAction = (TextView) findViewById(R.id.current_action);
        countingUpTotal = (TextView) findViewById(R.id.counting_up_total);
        countingDownTotal = (TextView) findViewById(R.id.counting_down_total);
        totalBar = (ProgressBar) findViewById(R.id.total_bar);

        currentWorkoutSet = WorkoutSet.loadC25kWorkouts(this);
        workoutIdx = getPreferences(MODE_PRIVATE).getInt(WORKOUT_IDX, 0);
        setUpCurrentWorkout();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.c25k_website:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.c25k_uri))));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void startPause(View view) {
        if (timer.isPaused()) {
            currentAction.setText(currentWorkoutSet.get(workoutIdx).getSegment(segmentIdx).getType().toString(this));
            startPause.setText(getString(R.string.pause));
            timer.start();
            disableBackForthButtons();
        } else {
            timer.pause();
            startPause.setText(getString(R.string.start));
            cancelVibrations();
            enableBackForthButtons();
        }
    }

    private void setUpCurrentWorkout() {
        segmentIdx = 0;
        title.setText(currentWorkoutSet.get(workoutIdx).getTitle());
        description.setText(Html.fromHtml(currentWorkoutSet.get(workoutIdx).getDescription()));
        startPause.setText(getString(R.string.start));
        setBackForthButtonsForCurrentWorkout();
        setUpProgressBarsForCurrentSegment();
        setUpTimerForCurrentSegment();
    }
    private void setUpTimerForCurrentSegment() {
        if (timer != null) {
            timer.cancel();
        }
        timer = makeTimer();
    }

    public void nextWorkout(View view) {
        if (BuildConfig.DEBUG && workoutIdx >= currentWorkoutSet.length() - 1) { throw new AssertionError(); }
        cancelVibrations();
        ++workoutIdx;
        persistWorkout(workoutIdx);
        setUpCurrentWorkout();
    }
    public void prevWorkout(View view) {
        if (BuildConfig.DEBUG && workoutIdx < 1) { throw new AssertionError(); }
        cancelVibrations();
        --workoutIdx;
        persistWorkout(workoutIdx);
        setUpCurrentWorkout();
    }

    private void persistWorkout(int idx) {
        getPreferences(MODE_PRIVATE).edit().putInt(WORKOUT_IDX, idx).apply();
    }

    private @NotNull PausableCountDownTimer makeTimer() {
        return new PausableCountDownTimer(currentWorkoutSet.get(workoutIdx).getSegment(segmentIdx).length(), 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                setProgressBars(millisUntilFinished);
                if (millisUntilFinished < 1000 * vibratingReminders) {
                    --vibratingReminders;
                    vibrate(warningPattern);
                }
            }

            @Override
            public void onFinish() {
                vibrate(endSegmentPattern);
                if (segmentIdx < currentWorkoutSet.get(workoutIdx).numberSegments() - 1) {
                    // Go to next segment of the workout
                    ++segmentIdx;
                    setUpProgressBarsForCurrentSegment();
                    setUpTimerForCurrentSegment();
                    timer.start();
                } else {
                    // Workout finished!
                    setProgressBars(0); // so it stays done unless they hit "start"
                    startPause.setText(getString(R.string.start));
                    enableBackForthButtons();
                    persistWorkout(workoutIdx + 1);
                    // So that if they hit "start" it restarts the workout
                    segmentIdx = 0;
                    setUpTimerForCurrentSegment();
                }
            }

            private final long[] endSegmentPattern = { 0, 750, 500, 750, 500, 750 };
            private final long[] warningPattern = { 0, 150 };
            private int vibratingReminders = Math.min(5, (int)(timerLife / 1000));
        };
    }

    private void setUpProgressBarsForCurrentSegment() {
        WorkoutSet.Segment currentSegment = currentWorkoutSet.get(workoutIdx).getSegment(segmentIdx);
        currentAction.setText(currentSegment.getType().toString(TimerActivity.this));
        setProgressBars(currentSegment.length());
    }
    private void setProgressBars(long millisToEnd) {
        long localTotal = currentWorkoutSet.get(workoutIdx).getSegment(segmentIdx).length();
        progressBar.setProgress((int)(((localTotal - millisToEnd) * 100) / localTotal));
        countingUp.setText(convertToMMSS(localTotal - millisToEnd));
        countingDown.setText(String.format(Locale.US, "-%s", convertToMMSS(millisToEnd + 999)));

        long totalElapsed = currentWorkoutSet.get(workoutIdx).getLengthUpTo(segmentIdx) + (localTotal - millisToEnd);
        long workoutLength = currentWorkoutSet.get(workoutIdx).getLength();
        totalBar.setProgress((int)((totalElapsed * 100) / workoutLength));
        countingUpTotal.setText(convertToMMSS(totalElapsed));
        countingDownTotal.setText(String.format(Locale.US, "-%s", convertToMMSS(workoutLength - totalElapsed + 999)));
    }

    private void setBackForthButtonsForCurrentWorkout() {
        forth.setVisibility((workoutIdx < currentWorkoutSet.length() - 1) ? View.VISIBLE : View.INVISIBLE);
        back.setVisibility((workoutIdx > 0) ? View.VISIBLE : View.INVISIBLE);
        enableBackForthButtons();
    }
    private void enableBackForthButtons() {
        forth.setEnabled(true);
        back.setEnabled(true);
    }
    private void disableBackForthButtons() {
        forth.setEnabled(false);
        back.setEnabled(false);
    }

    private static SimpleDateFormat df = new SimpleDateFormat("m:ss", Locale.US);
    static { df.setTimeZone(TimeZone.getTimeZone("UTC")); }
    private static @NotNull String convertToMMSS(long millis) {
        return df.format(new Date(millis));
    }

    private void vibrate(@NotNull long[] pattern) {
        final Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            v.vibrate(pattern, -1, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build());
        } else {
            v.vibrate(pattern, -1);
        }
    }
    private void cancelVibrations() {
        final Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        v.cancel();
    }
}
