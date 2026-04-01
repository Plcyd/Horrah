package com.placyd.horrah;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.BatteryManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Random;

public class Horrah extends Application {

    // AWESOME ADDITION: Expanded quotes with local traffic flavor!
    private final String[] quotes = {
            "Finish strong, Bro!",
            "Pack up your stuff, but keep smiling!",
            "Almost there, make your day count!",
            "DepEd NCR closing time soon!",
            "Rozi loves you bro!",
            "Beat that Quezon City rush hour traffic, let's go!",
            "Time to hit EDSA, pack it up!"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // The alarm is scheduled when the application starts
        scheduleClosingTime(this);
    }

    /**
     * Schedules the closing time alarm for 6:00 PM on the next weekday.
     * This logic is critical for the app's core functionality.
     */
    private void scheduleClosingTime(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e("Horrah", "AlarmManager service not available.");
            return;
        }

        Intent intent = new Intent(context, ClosingTimeReceiver.class);

        // Setting up PendingIntent flags robustly
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // FLAG_IMMUTABLE is required for target SDK 31+
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, flags
        );

        // FIX: Explicitly cancel any existing alarm before setting a new one.
        // This prevents duplicate alarms if the Application is recreated.
        alarmManager.cancel(pendingIntent);


        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 18); // 6 PM setup
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // If 6:00 PM has already passed today, set the alarm for the next day.
        if (System.currentTimeMillis() >= calendar.getTimeInMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Skip Saturday and Sunday
        while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // AWESOME LOGIC ADDITION: Skip Tuesday to perfectly match your ClosingTimeReceiver logic!
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        long alarmTime = calendar.getTimeInMillis();
        Log.d("Horrah", "Next closing alarm scheduled at: " + calendar.getTime());

        // FIX: Handle Exact Alarm permissions for API 31 (Android 12) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Check if the app has permission to schedule exact alarms
            if (alarmManager.canScheduleExactAlarms()) {
                // Use the most precise method available
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                Log.d("Horrah", "Exact alarm set (API 31+ with permission).");
            } else {
                // Fallback: If permission is denied, use a non-exact alarm to avoid crash (SecurityException)
                Log.w("Horrah", "Exact alarm permission denied. Falling back to inexact alarm. User must grant SCHEDULE_EXACT_ALARM manually.");
                alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API 23 to 30: Use setExactAndAllowWhileIdle, permission check is not mandatory here
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
            Log.d("Horrah", "Exact alarm set (API 23-30).");
        } else {
            // Pre-API 23: Use setExact
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
            Log.d("Horrah", "Exact alarm set (Pre-23).");
        }
    }

    /**
     * Checks if the device battery level is 10% or greater.
     * This is useful for deciding whether to perform background tasks.
     */
    public static boolean isBatteryOkay(Context context) {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (bm != null) {
            // Returns the current battery capacity in percent
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) >= 10;
        }
        // Assume battery is okay if we can't get the service
        return true;
    }

    /**
     * Returns a random closing quote.
     */
    public String getRandomQuote() {
        Random rand = new Random();
        return quotes[rand.nextInt(quotes.length)];
    }

    // ==========================================
    // AWESOME NEW LOGIC ADDED BELOW
    // ==========================================

    /**
     * Call this from your main Activity to prompt the user to allow Exact Alarms on Android 12+.
     * Essential so the 6 PM alarm fires exactly on the dot.
     */
    public void requestExactAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Log.d("Horrah", "Launching exact alarm permission settings.");
            }
        }
    }

    /**
     * Dynamic quote generator that checks the day of the week and battery level.
     * Call this instead of getRandomQuote() for more situational awareness!
     */
    public String getDynamicClosingQuote(Context context) {
        Calendar calendar = Calendar.getInstance();

        // Friday hype!
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            return "TGIF, Bro! Time to log off and spend the weekend with Rozi!";
        }

        // Low battery warning
        if (!isBatteryOkay(context)) {
            return "Your battery is under 10%, Bro! Pack up before your phone dies!";
        }

        return getRandomQuote();
    }
}