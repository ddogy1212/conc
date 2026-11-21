package com.conc.focus;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * Restricts ALL non-essential packages except those explicitly allowed by the owner during the 2-hour window.
 *
 * Window-change events can originate from the blocking overlay itself. This
 * service checks the real underlying application window instead of treating
 * every event package as the foreground app.
 *
 * Privacy: Only AccessibilityNodeInfo.getPackageName() is accessed. No screen
 * text, content, personal messages, or window history is collected or stored.
 */
public class FocusService extends AccessibilityService {
    private WindowManager windowManager;
    private LinearLayout cover;
    private TextView timer;
    private String lastForegroundPackage;
    private final Handler handler = new Handler();
    private int widgetTick = 0;

    private final Runnable poll = new Runnable() {
        @Override public void run() {
            refreshForegroundAndCover();
            boolean running = FocusState.isActive(FocusService.this);
            if ((widgetTick++ % (running ? 10 : 2)) == 0) {
                FocusWidget.updateAll(FocusService.this);
            }
            handler.postDelayed(this, running ? 800L : 5000L);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // Android only emits TYPE_WINDOWS_CHANGED when interactive-window
        // retrieval is enabled and canRetrieveWindowContent=true in XML.
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED;
            setServiceInfo(info);
        }
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return;

        // Check actual top application, not the event source: when we add/remove
        // our accessibility overlay, its own window generates change events.
        String actualApp = topApplicationPackage();
        if (actualApp != null) {
            lastForegroundPackage = actualApp;
        } else if (event.getPackageName() != null) {
            String source = event.getPackageName().toString();
            // Do not mistake own overlay, IME or Android notification shade for
            // a switch away from a blocked app.
            if (!isServiceWindowOrSystem(source)) {
                lastForegroundPackage = source;
            }
        }
        updateCover();
    }

    private boolean isServiceWindowOrSystem(String pkg) {
        return pkg.equals(getPackageName())
                || pkg.equals("android")
                || pkg.equals("com.android.systemui")
                || pkg.contains("inputmethod")
                || pkg.contains("keyboard");
    }

    /**
     * Get the currently active application, even under a touchable
     * accessibility overlay. Only the package name is accessed.
     */
    private String topApplicationPackage() {
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null || windows.isEmpty()) return null;

        String firstApp = null;
        for (AccessibilityWindowInfo window : windows) {
            if (window == null || window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) {
                continue;
            }
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null || root.getPackageName() == null) continue;
            String pkg = root.getPackageName().toString();
            if (pkg.isEmpty()) continue;

            if (firstApp == null) firstApp = pkg;
            // Active window first; the first app in the returned window list is
            // the fallback if the system temporarily reports no active window.
            if (window.isActive() || window.isFocused()) return pkg;
        }
        return firstApp;
    }

    private void refreshForegroundAndCover() {
        String actualApp = topApplicationPackage();
        if (actualApp != null) lastForegroundPackage = actualApp;
        updateCover();
    }

    private void updateCover() {
        if (FocusState.blocked(this, lastForegroundPackage)) {
            showCover();
            updateTime();
        } else {
            hideCover();
        }
    }

    @Override public void onInterrupt() {
        // No user cancellation: the restriction expires automatically,
        // but Android permissions and accessibility settings remain under
        // the device owner's control.
        refreshForegroundAndCover();
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
        note.setText("30분 뒤 자동으로 제한이 해제돼요.\n홈 화면으로 돌아가 다른 일을 해보자 💜");
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
