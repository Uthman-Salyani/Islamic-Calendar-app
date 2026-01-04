package com.islamiccalendar.islamiccalendarapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "hijri_calendar_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        HijriDateManager dateManager = new HijriDateManager(context);

        int currentDay = dateManager.getDay();

        // If it's the 29th day, send notification (don't auto-increment)
        if (currentDay == 29) {
            sendNotification(context);
        }
        // If it's the 30th day, move to next month automatically
        else if (currentDay == 30) {
            int currentMonth = dateManager.getMonth();
            int currentYear = dateManager.getYear();

            // Move to day 1 of next month
            int newMonth = currentMonth + 1;
            int newYear = currentYear;

            if (newMonth > 12) {
                newMonth = 1;
                newYear++;
            }

            dateManager.saveHijriDate(1, newMonth, newYear);

            // Update widget
            CalendarWidget.updateWidget(context);
        }
        // Auto-increment for days 1-28
        else if (currentDay < 29) {
            dateManager.incrementDate();

            // Update widget
            CalendarWidget.updateWidget(context);
        }

        // Schedule next day's alarm
        AlarmScheduler.scheduleNextAlarm(context);
    }

    private void sendNotification(Context context) {
        createNotificationChannel(context);

        // Create intent to open MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Islamic Calendar - Month End")
                .setContentText("It's the 29th of the month. Please check for moon sighting and set the new date.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Hijri Calendar Notifications";
            String description = "Notifications for month-end reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}