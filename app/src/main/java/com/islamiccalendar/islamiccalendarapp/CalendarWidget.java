package com.islamiccalendar.islamiccalendarapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class CalendarWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update all widgets
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Called when first widget is created
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        // Called when last widget is removed
        super.onDisabled(context);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        HijriDateManager dateManager = new HijriDateManager(context);

        // Create RemoteViews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // Update widget data
        views.setTextViewText(R.id.widgetDayNumber, dateManager.getArabicDay());
        views.setTextViewText(R.id.widgetArabicMonth, dateManager.getArabicMonthName());
        views.setTextViewText(R.id.widgetMonthName,
                dateManager.getEnglishMonthName() + " " + dateManager.getDay());
        views.setTextViewText(R.id.widgetYear, dateManager.getYear() + " A.H");

        // Create intent to open MainActivity when widget is clicked
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetLayout, pendingIntent);

        // Update widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    // Static method to update widget from other parts of the app
    public static void updateWidget(Context context) {
        Intent intent = new Intent(context, CalendarWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new android.content.ComponentName(context, CalendarWidget.class));

        if (appWidgetIds != null && appWidgetIds.length > 0) {
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            context.sendBroadcast(intent);

            // Also update directly
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }
}