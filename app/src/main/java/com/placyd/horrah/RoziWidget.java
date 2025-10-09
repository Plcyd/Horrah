package com.placyd.horrah;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RoziWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_rozi);

            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String date = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date());

            views.setTextViewText(R.id.clock_text, time);
            views.setTextViewText(R.id.date_text, date);

            Intent intent = new Intent(context, ClankWorker.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_rozi, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
