package com.islamiccalendar.islamiccalendarapp;

import android.content.Context;
import android.content.SharedPreferences;

public class HijriDateManager {
    private static final String PREFS_NAME = "HijriCalendarPrefs";
    private static final String KEY_DAY = "hijri_day";
    private static final String KEY_MONTH = "hijri_month";
    private static final String KEY_YEAR = "hijri_year";
    private static final String KEY_SUNSET_TIME = "sunset_time";
    private static final String KEY_LAST_UPDATE = "last_update_date";
    private static final String KEY_IS_FIRST_LAUNCH = "is_first_launch";

    private SharedPreferences prefs;
    private Context context;

    // Hijri month names in Arabic
    private static final String[] ARABIC_MONTH_NAMES = {
            "مُحَرَّم", "صَفَر", "رَبِيع ٱلْأَوَّل", "رَبِيع ٱلثَّانِي",
            "جُمَادَىٰ ٱلْأُولَىٰ", "جُمَادَىٰ ٱلثَّانِيَة", "رَجَب", "شَعْبَان",
            "رَمَضَان", "شَوَّال", "ذُو ٱلْقَعْدَة", "ذُو ٱلْحِجَّة"
    };

    // Hijri month names in English
    private static final String[] ENGLISH_MONTH_NAMES = {
            "MUHARRAM", "SAFAR", "RABI' AL-AWWAL", "RABI' AL-THANI",
            "JUMADA AL-ULA", "JUMADA AL-THANI", "RAJAB", "SHA'BAN",
            "RAMADAN", "SHAWWAL", "DHU AL-QI'DAH", "DHU AL-HIJJAH"
    };

    public HijriDateManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Save Hijri date
    public void saveHijriDate(int day, int month, int year) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_DAY, day);
        editor.putInt(KEY_MONTH, month);
        editor.putInt(KEY_YEAR, year);
        editor.putString(KEY_LAST_UPDATE, getCurrentDate());
        editor.apply();
    }

    // Get current Hijri day
    public int getDay() {
        return prefs.getInt(KEY_DAY, 1);
    }

    // Get current Hijri month (1-12)
    public int getMonth() {
        return prefs.getInt(KEY_MONTH, 1);
    }

    // Get current Hijri year
    public int getYear() {
        return prefs.getInt(KEY_YEAR, 1447);
    }

    // Get Arabic month name
    public String getArabicMonthName() {
        int month = getMonth();
        if (month >= 1 && month <= 12) {
            return ARABIC_MONTH_NAMES[month - 1];
        }
        return ARABIC_MONTH_NAMES[0];
    }

    // Get English month name
    public String getEnglishMonthName() {
        int month = getMonth();
        if (month >= 1 && month <= 12) {
            return ENGLISH_MONTH_NAMES[month - 1];
        }
        return ENGLISH_MONTH_NAMES[0];
    }

    // Get full date string (e.g., "RAJAB 15 1447 A.H")
    public String getFullDateString() {
        return getEnglishMonthName() + " " + getDay() + " " + getYear() + " A.H";
    }

    // Increment date by one day
    // Increment date by one day
    public void incrementDate() {
        int day = getDay();
        int month = getMonth();
        int year = getYear();

        day++;

        // If we go past 30, move to next month
        if (day > 30) {
            day = 1;
            month++;
            if (month > 12) {
                month = 1;
                year++;
            }
            saveHijriDate(day, month, year);
            return;
        }

        // Check if we've reached end of month (will be set by user on 29th/30th)
        // For automatic increment, we stop at 29 and wait for user input
        if (day > 29) {
            // Don't auto-increment past 29
            return;
        }

        saveHijriDate(day, month, year);
    }

    // Move to next month (called when user sets date to 1st)
    public void moveToNextMonth() {
        int month = getMonth();
        int year = getYear();

        month++;
        if (month > 12) {
            month = 1;
            year++;
        }

        saveHijriDate(1, month, year);
    }

    // Save sunset time
    public void saveSunsetTime(String time) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SUNSET_TIME, time);
        editor.apply();
    }

    // Get sunset time
    public String getSunsetTime() {
        return prefs.getString(KEY_SUNSET_TIME, "18:00");
    }

    // Check if date has been set
    public boolean isDateSet() {
        return prefs.contains(KEY_DAY);
    }

    // Get current date string for tracking updates
    private String getCurrentDate() {
        return String.valueOf(System.currentTimeMillis() / (1000 * 60 * 60 * 24));
    }

    // Check if this is first launch
    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true);
    }

    // Mark first launch as completed
    public void setFirstLaunchCompleted() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_FIRST_LAUNCH, false);
        editor.apply();
    }

    // Get last update date
    public String getLastUpdateDate() {
        return prefs.getString(KEY_LAST_UPDATE, getCurrentDate());
    }

    // Check if date should be updated (if current date is different from last update)
    public boolean shouldUpdateDate() {
        String lastUpdate = getLastUpdateDate();
        String currentDate = getCurrentDate();
        return !lastUpdate.equals(currentDate);
    }

    // Mark date as updated today
    public void markDateUpdated() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LAST_UPDATE, getCurrentDate());
        editor.apply();
    }

    // Convert Western numerals to Arabic-Indic numerals
    public String getArabicDay() {
        int day = getDay();
        return convertToArabicNumerals(String.valueOf(day));
    }

    // Helper method to convert numerals
    private String convertToArabicNumerals(String number) {
        char[] arabicNumerals = {'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'};
        StringBuilder result = new StringBuilder();

        for (char c : number.toCharArray()) {
            if (Character.isDigit(c)) {
                result.append(arabicNumerals[c - '0']);
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}