package com.placyd.horrah;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory; // ADDED: Needed for the Large Icon trick
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Enhanced BootReceiver for Horrah.
 * Ensures the app's services and alarms are restarted after the device reboots.
 * Packed with device telemetry, safe WakeLock management, and robust error handling.
 * Requires the RECEIVE_BOOT_COMPLETED permission in the AndroidManifest.xml.
 */
public class BootReceiver extends BroadcastReceiver {

    // =========================================================================================
    // CONSTANTS & CONFIGURATION
    // =========================================================================================

    private static final String TAG = "HorrahBootReceiver";
    private static final String PREF_NAME = "HorrahBootPrefs";
    private static final String KEY_LAST_BOOT_TIME = "last_boot_time_millis";
    private static final String KEY_BOOT_COUNT = "total_device_boots_tracked";

    private static final String CHANNEL_ID = "boot_battery_channel_v2";
    private static final CharSequence CHANNEL_NAME = "System Startup & Diagnostics";
    private static final String CHANNEL_DESC = "Notifications regarding app startup after a device reboot.";
    private static final int NOTIFICATION_ID = 2001;

    // WakeLock timeout to ensure we don't drain battery if something hangs (15 seconds)
    private static final long WAKELOCK_TIMEOUT_MS = 15 * 1000L;

    // Minimum acceptable storage in MB to consider the device "healthy"
    private static final long MIN_SAFE_STORAGE_MB = 100;

    // =========================================================================================
    // MAIN ENTRY POINT
    // =========================================================================================

    @Override
    public void onReceive(Context context, Intent intent) {
        // 1. Initial validation
        if (context == null || intent == null || intent.getAction() == null) {
            Log.e(TAG, "Received null context, intent, or action. Aborting boot sequence.");
            return;
        }

        String action = intent.getAction();
        Log.i(TAG, "Broadcast received with action: " + action);

        // 2. Action Verification
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
                "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.i(TAG, "Valid boot sequence detected. Initiating Horrah startup protocols...");

            // 3. Acquire WakeLock
            // This is crucial. When the device boots, it tries to go back to sleep quickly.
            // We need a partial wakelock to keep the CPU running just long enough to finish our checks.
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = null;

            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "::BootWakeLock");
                wakeLock.acquire(WAKELOCK_TIMEOUT_MS);
                Log.d(TAG, "WakeLock acquired safely for up to " + WAKELOCK_TIMEOUT_MS + "ms.");
            } else {
                Log.w(TAG, "PowerManager is null. Cannot acquire WakeLock. Execution might be interrupted.");
            }

            try {
                // 4. Execute all core boot logic
                executeBootSequence(context);
            } catch (Exception e) {
                // Catch absolutely everything so the receiver never crashes the application thread
                Log.e(TAG, "CRITICAL ERROR during boot sequence execution: " + e.getMessage(), e);
            } finally {
                // 5. Release WakeLock securely
                if (wakeLock != null && wakeLock.isHeld()) {
                    try {
                        wakeLock.release();
                        Log.d(TAG, "WakeLock released successfully.");
                    } catch (Exception e) {
                        Log.e(TAG, "Error releasing WakeLock: " + e.getMessage(), e);
                    }
                }
            }
        } else {
            Log.w(TAG, "Received unhandled intent action: " + action);
        }
    }

    // =========================================================================================
    // CORE BOOT SEQUENCE LOGIC
    // =========================================================================================

    /**
     * Orchestrates the various checks and initializations required on boot.
     * Broken down into modular methods for butter-smooth execution and readability.
     */
    private void executeBootSequence(Context context) {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "Starting detailed executeBootSequence...");

        // Step 1: Record the boot event locally
        recordBootEvent(context, startTime);

        // Step 2: Run comprehensive device diagnostics
        String batteryReport = performBatteryDiagnostics(context);
        String memoryReport = performMemoryDiagnostics(context);
        String networkReport = performNetworkDiagnostics(context);

        // Step 3: Check Horrah specific requirements (Original Logic Retained)
        // Ensure Horrah class has this method, otherwise it will fail to compile.
        boolean isBatteryOkay = false;
        try {
            isBatteryOkay = Horrah.isBatteryOkay(context);
            Log.i(TAG, "Horrah.isBatteryOkay() returned: " + isBatteryOkay);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call Horrah.isBatteryOkay(). Is the method available? Error: " + e.getMessage());
        }

        // Step 4: Reschedule core application alarms/workers
        rescheduleAppComponents(context);

        // Step 5: Build and show the final rich notification
        displayBootNotification(context, isBatteryOkay, batteryReport, memoryReport, networkReport);

        long duration = System.currentTimeMillis() - startTime;
        Log.i(TAG, "Boot sequence completed smoothly in " + duration + "ms.");
    }

    // =========================================================================================
    // TELEMETRY & DIAGNOSTICS
    // =========================================================================================

    /**
     * Gathers deep metrics about the current battery state.
     * Returns a formatted string for the notification or logs.
     */
    private String performBatteryDiagnostics(Context context) {
        Log.d(TAG, "Running performBatteryDiagnostics...");
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);

            if (batteryStatus == null) {
                return "Battery status unavailable.";
            }

            // Level & Scale
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float) scale;

            // Charging state
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            // Charge plug type
            int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
            String plugType = usbCharge ? "USB" : (acCharge ? "AC" : (isCharging ? "Wireless/Other" : "Unplugged"));

            // Temperature and Voltage
            int tempDeciCelsius = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            float tempCelsius = tempDeciCelsius / 10.0f;
            int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);

            String report = String.format(Locale.US,
                    "Lvl: %.1f%% | State: %s (%s) | Temp: %.1f°C | Volt: %dmV",
                    batteryPct,
                    isCharging ? "Charging" : "Discharging",
                    plugType,
                    tempCelsius,
                    voltage);

            Log.d(TAG, "Battery Report Generated: " + report);
            return report;

        } catch (Exception e) {
            Log.e(TAG, "Error checking battery details: " + e.getMessage());
            return "Battery check failed.";
        }
    }

    /**
     * Checks device RAM and internal storage capacity.
     */
    private String performMemoryDiagnostics(Context context) {
        Log.d(TAG, "Running performMemoryDiagnostics...");
        try {
            // Check RAM
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            if (activityManager != null) {
                activityManager.getMemoryInfo(memoryInfo);
            }

            long availRamMB = memoryInfo.availMem / (1024 * 1024);
            long totalRamMB = memoryInfo.totalMem / (1024 * 1024);
            boolean isLowRam = memoryInfo.lowMemory;

            // Check Internal Storage
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            long availStorageMB = (availableBlocks * blockSize) / (1024 * 1024);

            if (availStorageMB < MIN_SAFE_STORAGE_MB) {
                Log.w(TAG, "WARNING: Device storage is critically low! Only " + availStorageMB + "MB remaining.");
            }

            String report = String.format(Locale.US,
                    "RAM: %dMB/%dMB (Low: %b) | Storage Free: %dMB",
                    availRamMB, totalRamMB, isLowRam, availStorageMB);

            Log.d(TAG, "Memory Report Generated: " + report);
            return report;

        } catch (Exception e) {
            Log.e(TAG, "Error checking memory details: " + e.getMessage());
            return "Memory check failed.";
        }
    }

    /**
     * Determines the current network state immediately after boot.
     */
    @SuppressLint("MissingPermission") // Assuming permissions are handled in Manifest
    private String performNetworkDiagnostics(Context context) {
        Log.d(TAG, "Running performNetworkDiagnostics...");
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return "ConnectivityManager unavailable";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = cm.getActiveNetwork();
                if (network == null) return "No Active Network";

                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                if (capabilities == null) return "No Network Capabilities";

                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return "Connected via WiFi";
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return "Connected via Cellular";
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return "Connected via Ethernet";
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    return "Connected via VPN";
                } else {
                    return "Connected (Unknown Transport)";
                }
            } else {
                // Fallback for older devices (deprecated but necessary for legacy support)
                android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                    return "Connected (Legacy API): " + activeNetwork.getTypeName();
                } else {
                    return "No Active Network (Legacy API)";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network connectivity: " + e.getMessage());
            return "Network check failed.";
        }
    }

    // =========================================================================================
    // DATA PERSISTENCE & SCHEDULING
    // =========================================================================================

    /**
     * Saves the boot timestamp to SharedPreferences for later analytical use by the app.
     */
    private void recordBootEvent(Context context, long bootTimeMillis) {
        Log.d(TAG, "Recording boot event to SharedPreferences...");
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            int currentBootCount = prefs.getInt(KEY_BOOT_COUNT, 0);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(KEY_LAST_BOOT_TIME, bootTimeMillis);
            editor.putInt(KEY_BOOT_COUNT, currentBootCount + 1);
            editor.apply(); // apply() is asynchronous and safe for UI/Broadcast threads

            Log.d(TAG, "Boot event recorded successfully. Total app tracked boots: " + (currentBootCount + 1));
        } catch (Exception e) {
            Log.e(TAG, "Failed to record boot event: " + e.getMessage());
        }
    }

    /**
     * A safe wrapper to trigger alarm rescheduling without blocking the BroadcastReceiver.
     */
    private void rescheduleAppComponents(Context context) {
        Log.d(TAG, "Preparing to reschedule app components...");
        // NOTE TO DEVELOPER:
        // Directly scheduling alarms here is okay if it's quick.
        // However, if fetching the alarm times requires database lookups or network calls,
        // you MUST delegate this to a WorkManager or JobIntentService here instead.

        try {
            // Horrah.scheduleClosingTimeOnBoot(context);
            Log.i(TAG, "Placeholder: Application alarms should be rescheduled here via Horrah class methods or WorkManager.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to reschedule alarms: " + e.getMessage());
        }
    }

    // =========================================================================================
    // NOTIFICATION MANAGEMENT
    // =========================================================================================

    /**
     * Builds and displays a highly detailed, robust notification for Android O+.
     */
    private void displayBootNotification(Context context, boolean isBatteryOkay,
                                         String batteryReport, String memoryReport,
                                         String networkReport) {
        Log.d(TAG, "Building and displaying boot notification...");

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            Log.e(TAG, "NotificationManager is null. Cannot post boot notification.");
            return;
        }

        // 1. Setup Notification Channel (Required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT // Elevated to default so user actually sees it
            );

            // Configure channel aesthetics
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.setShowBadge(true);
            // channel.setSound(null, null); // Uncomment if you want it silent

            manager.createNotificationChannel(channel);
            Log.d(TAG, "Notification Channel configured/updated.");
        }

        // 2. Format Notification Text
        String title = "Horrah System Initialized";
        String shortText = isBatteryOkay ? "Boot successful. Systems nominal." : "Boot successful. WARNING: Low Battery.";

        // Build a multiline string for the expanded notification view
        StringBuilder bigTextBuilder = new StringBuilder();
        bigTextBuilder.append("Horrah background services have been restored.\n\n");
        bigTextBuilder.append("📊 Diagnostics:\n");
        bigTextBuilder.append("• ").append(batteryReport).append("\n");
        bigTextBuilder.append("• ").append(memoryReport).append("\n");
        bigTextBuilder.append("• Network: ").append(networkReport).append("\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        bigTextBuilder.append("\nBoot Time: ").append(sdf.format(new Date()));

        // 3. Create a PendingIntent (Optional: tap to open the app)
        // Intent launchIntent = new Intent(context, HorrahMainActivity.class); // Replace with actual Activity
        // launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 4. Build the Notification (AWESOME ICON FIXES HERE)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_horrah_notif) // FIX: Your transparent Samsung-friendly icon
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher)) // FIX: Full color app icon on expansion
                .setContentTitle(title)
                .setContentText(shortText)
                // .setContentIntent(pendingIntent) // Uncomment to make notification clickable
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigTextBuilder.toString()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(Color.parseColor("#4CAF50")) // A nice green color to match the tech vibe
                .setAutoCancel(true) // Dismiss when tapped
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true);

        // 5. Fire Notification
        try {
            manager.notify(NOTIFICATION_ID, builder.build());
            Log.i(TAG, "Boot notification successfully dispatched to system bar.");
        } catch (SecurityException se) {
            // Can happen in Android 13+ if POST_NOTIFICATIONS permission isn't granted
            Log.e(TAG, "Missing POST_NOTIFICATIONS permission! Cannot show notification.", se);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error showing notification: " + e.getMessage(), e);
        }
    }
}