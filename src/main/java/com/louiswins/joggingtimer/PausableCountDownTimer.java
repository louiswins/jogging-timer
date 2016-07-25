package com.louiswins.joggingtimer;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import java.lang.ref.WeakReference;

public abstract class PausableCountDownTimer {
    public abstract void onTick(long millisUntilFinished);
    public abstract void onFinish();

    public PausableCountDownTimer(long millisInFuture, long countDownInterval) {
        timerLife = millisInFuture;
        period = countDownInterval;
    }

    public synchronized PausableCountDownTimer start() {
        if (cancelled) return this;
        paused = false;
        if (timerLife <= 0) {
            onFinish();
        } else {
            destTime = SystemClock.elapsedRealtime() + timerLife;
            handler.sendEmptyMessage(MESSAGE_WHAT);
        }
        return this;
    }

    public synchronized void pause() {
        if (paused || cancelled) return;
        timerLife = destTime - SystemClock.elapsedRealtime();
        paused = true;
    }
    public boolean isPaused() { return paused; }

    public synchronized void cancel() {
        cancelled = true;
        handler.removeMessages(MESSAGE_WHAT);
    }

    private static final class PCTHandler extends Handler {
        public PCTHandler(PausableCountDownTimer that) {
            weakThat = new WeakReference<>(that);
        }

        @Override
        @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
        public void handleMessage(Message m) {
            PausableCountDownTimer that = weakThat.get();
            if (that == null) return;
            synchronized (that) {
                if (that.paused || that.cancelled) return;

                long lastTick = SystemClock.elapsedRealtime();
                long millisLeft = that.destTime - lastTick;
                if (millisLeft <= 0) {
                    that.onFinish();
                } else if (millisLeft < that.period) {
                    sendEmptyMessageDelayed(MESSAGE_WHAT, millisLeft);
                } else {
                    that.onTick(millisLeft);
                    if (that.cancelled || that.paused) return;
                    long delay = lastTick + that.period - SystemClock.elapsedRealtime();
                    while (delay < 0) delay += that.period;
                    sendEmptyMessageDelayed(MESSAGE_WHAT, delay);
                }
            }
        }

        private WeakReference<PausableCountDownTimer> weakThat;
    }
    private Handler handler = new PCTHandler(this);

    private final static int MESSAGE_WHAT = 42;
    private boolean cancelled = false;
    private boolean paused = true;

    protected final long period;
    // Use timerLife before we start or when resuming from pause
    protected long timerLife;
    // Use destTime when timer is running
    protected long destTime;
}
