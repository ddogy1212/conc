package com.conc.focus;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class FocusService extends AccessibilityService {
    private WindowManager windowManager;
    private LinearLayout cover;
    private TextView timer;
    private String lastForegroundPackage;
    private final Handler handler = new Handler();
    private int widgetTick = 0;
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            boolean running = FocusState.isActive(FocusService.this);
            if (running && FocusState.blocked(FocusService.this, lastForegroundPackage)) {
                showCover();
                updateTime();
            } else {
                hideCover();
            }
            if ((widgetTick++ % 10) == 0) FocusWidget.updateAll(FocusService.this);
            handler.postDelayed(this, running ? 1000L : 15000L);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        if (!pkg.equals(lastForegroundPackage)) lastForegroundPackage = pkg;
        if (FocusState.blocked(this, pkg)) {
            showCover();
            updateTime();
        } else if (!FocusState.isActive(this) || FocusState.isProtected(pkg) ||
                !FocusState.chosenApps(this).contains(pkg)) {
            hideCover();
        }
    }

    @Override public void onInterrupt() {
        hideCover();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showCover() {
        if (cover != null || windowManager == null) return;
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(25), dp(30), dp(25), dp(30));
        GradientDrawable backdrop = new GradientDrawable();
        backdrop.setColor(Color.rgb(33, 21, 52));
        container.setBackground(backdrop);

        TextView mark = new TextView(this);
        mark.setText("✦  CONC  ✦");
        mark.setTextSize(22);
        mark.setTypeface(null, Typeface.BOLD);
        mark.setTextColor(Color.rgb(212, 176, 255));
        mark.setGravity(Gravity.CENTER);
        container.addView(mark);
        TextView headline = new TextView(this);
        headline.setText("지금은 집중할 시간!");
        headline.setTextSize(25);
        headline.setTypeface(null, Typeface.BOLD);
        headline.setTextColor(Color.WHITE);
        headline.setGravity(Gravity.CENTER);
        headline.setPadding(0, dp(17), 0, dp(14));
        container.addView(headline);

        timer = new TextView(this);
        timer.setTextColor(Color.WHITE);
        timer.setTextSize(43);
        timer.setTypeface(null, Typeface.BOLD);
        timer.setGravity(Gravity.CENTER);
        container.addView(timer);

        TextView note = new TextView(this);
        note.setText("30분이 끝나면 자동으로 열려요.\n홈 화면으로 돌아가 다른 일을 해보자 💜");
        note.setTextColor(Color.rgb(213, 201, 232));
        note.setTextSize(15);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(20), 0, 0);
        container.addView(note);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        try {
            windowManager.addView(container, params);
            cover = container;
        } catch (RuntimeException e) {
            cover = null;
            timer = null;
        }
    }

    private void updateTime() {
        if (timer == null) return;
        long secs = (FocusState.remainingMillis(this) + 999L) / 1000L;
        timer.setText(String.format(Locale.KOREA, "%02d:%02d", secs / 60, secs % 60));
    }

    private void hideCover() {
        if (cover == null || windowManager == null) return;
        try { windowManager.removeViewImmediate(cover); }
        catch (RuntimeException ignored) {}
        cover = null;
        timer = null;
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(poll);
        hideCover();
        super.onDestroy();
    }
}
