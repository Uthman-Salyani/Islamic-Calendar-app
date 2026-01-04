package com.islamiccalendar.islamiccalendarapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;

public class DailyUpdateWorker extends Worker {

    private static final String CHANNEL_ID = "hijri_calendar_channel";
    private static final int NOTIFICATION_ID = 1;

    public DailyUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        HijriDateManager dateManager = new HijriDateManager(context);

        // Get current time
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        // Get sunset time
        String sunsetTime = dateManager.getSunsetTime();
        String[] timeParts = sunsetTime.split(":");
        int sunsetHour = Integer.parseInt(timeParts[0]);
        int sunsetMinute = Integer.parseInt(timeParts[1]);

        // Check if current time is at or past sunset time
        boolean isSunsetTime = (currentHour > sunsetHour) ||
                (currentHour == sunsetHour && currentMinute >= sunsetMinute);

        // Only update if it's sunset time and we haven't updated today
        if (isSunsetTime && dateManager.shouldUpdateDate()) {
            int currentDay = dateManager.getDay();

            // If it's the 29th day, send notification instead of auto-incrementing
            if (currentDay == 29) {
                sendNotification(context);
            } else if (currentDay < 29) {
                // Auto-increment for days 1-28
                dateManager.incrementDate();
                dateManager.markDateUpdated();

                // Update widget
                CalendarWidget.updateWidget(context);
            }
        }

        return Result.success();
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