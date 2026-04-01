package com.placyd.horrah;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AppWidgetProvider for Rozi Clock Widget.
 * This class handles displaying the current time and date, and uses AlarmManager
 * to schedule frequent updates since onUpdate() is not called often enough for a clock.
 */
public class RoziWidget extends AppWidgetProvider {

    // Define a unique action for frequent widget updates
    public static final String WIDGET_UPDATE_ACTION = "com.placyd.horrah.WIDGET_UPDATE_ACTION";

    /**
     * Updates a single instance of the widget with the current time and date.
     */
    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Create RemoteViews using the widget layout
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_rozi);

        // Format and set the time and date
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String date = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date());

        views.setTextViewText(R.id.clock_text, time);
        views.setTextViewText(R.id.date_text, date);

        // FIX 1: Set click intent to launch the main app activity (FullscreenActivity)
        Intent intent = new Intent(context, FullscreenActivity.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                flags
        );
        // R.id.widget_rozi is the ID of the root layout defined in widget_rozi.xml
        views.setOnClickPendingIntent(R.id.widget_rozi, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * Called when the widget is first created and when the update interval expires.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * FIX 2: Handles custom broadcasts, particularly our WIDGET_UPDATE_ACTION from AlarmManager.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (WIDGET_UPDATE_ACTION.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, RoziWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            // Trigger the onUpdate logic to refresh the time on all instances
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    /**
     * FIX 3: Called when the first instance of the widget is created.
     * Starts the repeating alarm for frequent updates (every minute).
     */
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getAlarmPendingIntent(context);

        // Set up a repeating alarm to update the widget every 60 seconds (1 minute).
        long interval = 60 * 1000L;

        // Use setInexactRepeating for better battery life
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + interval, // Start immediately + 1 minute
                interval,
                pendingIntent
        );
    }

    /**
     * FIX 4: Called when the last instance of the widget is removed.
     * Cancels the repeating alarm.
     */
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getAlarmPendingIntent(context));
    }

    /**
     * Helper method to get the PendingIntent for the AlarmManager.
     */
    private PendingIntent getAlarmPendingIntent(Context context) {
        Intent intent = new Intent(context, RoziWidget.class);
        intent.setAction(WIDGET_UPDATE_ACTION);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Needed for API 23+ (Marshmallow) and mandatory for API 31+
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(
                context,
                0, // Request code
                intent,
                flags
        );
    }
}
