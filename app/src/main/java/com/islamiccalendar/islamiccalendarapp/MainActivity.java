package com.islamiccalendar.islamiccalendarapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private HijriDateManager dateManager;
    private TextView arabicMonthName;
    private TextView dayNumber;
    private TextView fullDateDisplay;
    private EditText hijriDateInput;
    private EditText sunsetTimeInput;
    private Button submitDateButton;
    private Button submitTimeButton;

    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize date manager
        dateManager = new HijriDateManager(this);

        // Initialize views
        arabicMonthName = findViewById(R.id.arabicMonthName);
        dayNumber = findViewById(R.id.dayNumber);
        fullDateDisplay = findViewById(R.id.fullDateDisplay);
        hijriDateInput = findViewById(R.id.hijriDateInput);
        sunsetTimeInput = findViewById(R.id.sunsetTimeInput);
        submitDateButton = findViewById(R.id.submitDateButton);
        submitTimeButton = findViewById(R.id.submitTimeButton);

        // Check if first launch and request notification permission
        if (dateManager.isFirstLaunch()) {
            requestNotificationPermission();
            dateManager.setFirstLaunchCompleted();
        }

        // Update display
        updateDisplay();

        // Set sunset time from saved preferences
        // Display sunset time in 12-hour format with AM/PM
        sunsetTimeInput.setText(convertTo12HourFormat(dateManager.getSunsetTime()));

        // Setup button listeners
        submitDateButton.setOnClickListener(v -> handleDateSubmit());
        submitTimeButton.setOnClickListener(v -> handleTimeSubmit());

        // Schedule daily updates
        scheduleDailyUpdates();
    }

    private void updateDisplay() {
        arabicMonthName.setText(dateManager.getArabicMonthName());
        dayNumber.setText(dateManager.getArabicDay()); // Use Arabic numerals
        fullDateDisplay.setText(dateManager.getFullDateString());

        // Update the input field to show current date with hyphens
        String currentDate = dateManager.getDay() + " - " +
                dateManager.getMonth() + " - " +
                dateManager.getYear();
        hijriDateInput.setText(currentDate);
    }


    private void handleDateSubmit() {
        String input = hijriDateInput.getText().toString().trim();

        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter a date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse input (format: DD MM YYYY)
        // Remove hyphens and split by spaces
        String[] parts = input.replace("-", "").trim().split("\\s+");

        if (parts.length != 3) {
            Toast.makeText(this, "Please enter date in format: DD MM YYYY", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            // Validate input
            if (day < 1 || day > 30) {
                Toast.makeText(this, "Day must be between 1 and 30", Toast.LENGTH_SHORT).show();
                return;
            }

            if (month < 1 || month > 12) {
                Toast.makeText(this, "Month must be between 1 and 12", Toast.LENGTH_SHORT).show();
                return;
            }

            if (year < 1) {
                Toast.makeText(this, "Please enter a valid year", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if moving to next month
            // When user sets day to 1 after being on day 29 or 30, move to next month
            int previousDay = dateManager.getDay();
            if (day == 1 && (previousDay == 29 || previousDay == 30)) {
                int currentMonth = dateManager.getMonth();
                int currentYear = dateManager.getYear();

                // Move to next month
                int newMonth = currentMonth + 1;
                int newYear = currentYear;

                if (newMonth > 12) {
                    newMonth = 1;
                    newYear++;
                }

                dateManager.saveHijriDate(1, newMonth, newYear);
            } else {
                // Save date normally
                dateManager.saveHijriDate(day, month, year);
            }

            // Update display
            updateDisplay();


            // Update widget
            CalendarWidget.updateWidget(this);

            // Reschedule alarm with potentially new date
            AlarmScheduler.scheduleNextAlarm(this);

            Toast.makeText(this, "Date updated successfully", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleTimeSubmit() {
        String time = sunsetTimeInput.getText().toString().trim().toUpperCase();

        if (time.isEmpty()) {
            Toast.makeText(this, "Please enter sunset time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if time contains AM/PM
        boolean hasAMPM = time.contains("AM") || time.contains("PM");

        String time24;
        if (hasAMPM) {
            // Validate 12-hour format (H:MM AM/PM or HH:MM AM/PM)
            if (!time.matches("^(1[0-2]|0?[1-9]):[0-5][0-9]\\s*(AM|PM)$")) {
                Toast.makeText(this, "Please enter time in format: HH:MM AM/PM", Toast.LENGTH_SHORT).show();
                return;
            }
            // Convert to 24-hour format for storage
            time24 = convertTo24HourFormat(time);
        } else {
            // Validate 24-hour format
            if (!time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                Toast.makeText(this, "Please enter time in format: HH:MM AM/PM or HH:MM (24-hour)", Toast.LENGTH_SHORT).show();
                return;
            }
            time24 = time;
        }

        // Save sunset time in 24-hour format
        dateManager.saveSunsetTime(time24);

        // Update display to show 12-hour format
        sunsetTimeInput.setText(convertTo12HourFormat(time24));

        // Reschedule daily updates with new time
        scheduleDailyUpdates();

        Toast.makeText(this, "Sunset time updated successfully", Toast.LENGTH_SHORT).show();
    }

    private void scheduleDailyUpdates() {
        // Schedule exact alarm for sunset time
        AlarmScheduler.scheduleNextAlarm(this);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Show explanation dialog
                new AlertDialog.Builder(this)
                        .setTitle("Enable Notifications")
                        .setMessage("This app needs notification permission to alert you on the 29th of each Hijri month to manually set the next month's date.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            ActivityCompat.requestPermissions(
                                    MainActivity.this,
                                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                    NOTIFICATION_PERMISSION_CODE
                            );
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. You won't receive reminders on the 29th.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Convert 24-hour time to 12-hour format with AM/PM
    private String convertTo12HourFormat(String time24) {
        try {
            String[] parts = time24.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            String period = "AM";
            if (hour >= 12) {
                period = "PM";
                if (hour > 12) {
                    hour -= 12;
                }
            }
            if (hour == 0) {
                hour = 12;
            }

            return String.format("%d:%02d %s", hour, minute, period);
        } catch (Exception e) {
            return time24;
        }
    }

    // Convert 12-hour format with AM/PM to 24-hour format
    private String convertTo24HourFormat(String time12) {
        try {
            String timeStr = time12.trim().toUpperCase();
            String period = timeStr.substring(timeStr.length() - 2); // Get AM or PM
            String timePart = timeStr.substring(0, timeStr.length() - 2).trim();

            String[] parts = timePart.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            if (period.equals("PM") && hour != 12) {
                hour += 12;
            } else if (period.equals("AM") && hour == 12) {
                hour = 0;
            }

            return String.format("%02d:%02d", hour, minute);
        } catch (Exception e) {
            return time12;
        }
    }
}