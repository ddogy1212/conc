package com.conc.focus;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;

public class FocusWidget extends AppWidgetProvider {
    public static final String ACTION_START = "com.conc.focus.START";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_START.equals(intent.getAction())) {
            if (!FocusState.isActive(context)) {
                if (!FocusState.begin(context)) {
                    Toast.makeText(context, "먼저 CONC 앱에서 차단 앱과 접근성 권한을 설정해줘!",
                            Toast.LENGTH_LONG).show();
                    Intent setup = new Intent(context, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(setup);
                }
            }
            updateAll(context);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        for (int id : widgetIds) manager.updateAppWidget(id, views(context));
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, FocusWidget.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids.length == 0) return;
        RemoteViews views = views(context);
        for (int id : ids) manager.updateAppWidget(id, views);
    }

    private static RemoteViews views(Context context) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.conc_widget);
        boolean active = FocusState.isActive(context);
        rv.setTextViewText(R.id.widget_status, active ? (int) Math.ceil(
                FocusState.remainingMillis(context) / 60000.0) + "분" : "시작");
        rv.setTextViewText(R.id.widget_title, active ? "FOCUS" : "CONC");

        File photo = new File(context.getFilesDir(), "chosen_widget.png");
        if (photo.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(photo.getAbsolutePath());
            if (bitmap != null) rv.setImageViewBitmap(R.id.widget_image, bitmap);
        } else {
            rv.setImageViewResource(R.id.widget_image, R.drawable.ic_conc);
        }

        Intent click = new Intent(context, FocusWidget.class).setAction(ACTION_START);
        PendingIntent pending = PendingIntent.getBroadcast(context, 44, click,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.widget_root, pending);
        return rv;
    }
}
