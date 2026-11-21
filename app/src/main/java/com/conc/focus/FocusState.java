package com.conc.focus;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class FocusState {
    private static final String PREFS = "conc_focus_prefs";
    private static final long DURATION = 30 * 60 * 1000L;

    private FocusState() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static Set<String> allowedApps(Context context) {
        return new HashSet<>(prefs(context).getStringSet("allowed_apps", Collections.emptySet()));
    }

    public static void saveAllowedApps(Context context, Set<String> apps) {
        if (isActive(context)) return;
        prefs(context).edit().putStringSet("allowed_apps", new HashSet<>(apps)).apply();
    }

    public static boolean isProtected(String pkg) {
        if (pkg == null || pkg.equals("com.conc.focus")) return true;
        if (pkg.equals("android") || pkg.equals("com.android.systemui")) return true;
        if (pkg.contains("permissioncontroller") || pkg.contains("emergency")) return true;
        if (pkg.equals("com.android.settings") || pkg.equals("com.android.phone")) return true;
        if (pkg.equals("com.android.dialer") || pkg.equals("com.google.android.dialer")
                || pkg.equals("com.samsung.android.dialer")) return true;
        if (pkg.equals("com.sec.android.app.launcher")
                || pkg.equals("com.google.android.apps.nexuslauncher")) return true;
        return false;
    }

    public static boolean isServiceEnabled(Context context) {
        AccessibilityManager manager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager == null) return false;
        for (AccessibilityServiceInfo info :
                manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (info.getResolveInfo() != null && info.getResolveInfo().serviceInfo != null) {
                String pkg = info.getResolveInfo().serviceInfo.packageName;
                String name = info.getResolveInfo().serviceInfo.name;
                if ("com.conc.focus".equals(pkg) && name.endsWith("FocusService")) return true;
            }
        }
        return false;
    }

    public static boolean isActive(Context context) {
        SharedPreferences p = prefs(context);
        long untilElapsed = p.getLong("until_elapsed", 0L);
        long startedElapsed = p.getLong("started_elapsed", 0L);
        long untilWall = p.getLong("until_wall", 0L);
        if (untilElapsed == 0L) return false;

        long nowElapsed = SystemClock.elapsedRealtime();
        boolean active = nowElapsed >= startedElapsed
                ? nowElapsed < untilElapsed
                : System.currentTimeMillis() < untilWall;
        if (!active) p.edit().remove("until_elapsed").remove("started_elapsed")
                .remove("until_wall").apply();
        return active;
    }

    public static long remainingMillis(Context context) {
        if (!isActive(context)) return 0L;
        SharedPreferences p = prefs(context);
        long nowElapsed = SystemClock.elapsedRealtime();
        long startedElapsed = p.getLong("started_elapsed", 0L);
        long remainder = nowElapsed >= startedElapsed
                ? p.getLong("until_elapsed", 0L) - nowElapsed
                : p.getLong("until_wall", 0L) - System.currentTimeMillis();
        return Math.max(0L, remainder);
    }

    public static boolean blocked(Context context, String pkg) {
        return isActive(context) && !isProtected(pkg) && !allowedApps(context).contains(pkg);
    }

    public static boolean begin(Context context) {
        if (isActive(context)) return true;
        if (!isServiceEnabled(context)) return false;

        long nowElapsed = SystemClock.elapsedRealtime();
        long until = nowElapsed + DURATION;
        prefs(context).edit().putLong("started_elapsed", nowElapsed)
                .putLong("until_elapsed", until)
                .putLong("until_wall", System.currentTimeMillis() + DURATION).commit();

        try {
            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarm != null) {
                Intent intent = new Intent(context, FocusTimerReceiver.class);
                PendingIntent pi = PendingIntent.getBroadcast(context, 105, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, until, pi);
            }
        } catch (RuntimeException ignored) {}
        FocusWidget.updateAll(context);
        return true;
    }
}
