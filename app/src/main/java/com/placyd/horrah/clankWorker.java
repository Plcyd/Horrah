    package com.placyd.horrah;

    import android.app.NotificationChannel;
    import android.app.NotificationManager;
    import android.content.Context;
    import android.content.Intent;
    import android.content.IntentFilter;
    import android.media.MediaPlayer;
    import android.os.BatteryManager;
    import android.os.Build;
    import android.os.Handler;
    import android.os.Looper;
    import android.widget.Toast;
    import androidx.annotation.NonNull;
    import androidx.core.app.NotificationCompat;
    import androidx.work.Worker;
    import androidx.work.WorkerParameters;
    import java.text.SimpleDateFormat;
    import java.util.Calendar;
    import java.util.Date;
    import java.util.Locale;
    import java.util.Random;

    // FIX 1: The class MUST be public, otherwise WorkManager will crash your app!
    public class clankWorker extends Worker {

        private final Context context;
        private static final String CHANNEL_ID = "closing_time_channel";

        public clankWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
            this.context = context;
        }

        @NonNull
        @Override
        public Result doWork() {
            Calendar now = Calendar.getInstance();
            int day = now.get(Calendar.DAY_OF_WEEK);

            // FIX 2: Brought back your Tuesday skip logic so it matches the rest of the app!
            if (day == Calendar.TUESDAY || day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
                return Result.success();
            }

            if (!checkBattery()) return Result.success();

            String formattedTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String formattedDate = new SimpleDateFormat("EEEE, MMM dd yyyy", Locale.getDefault()).format(new Date());
            String randomQuote = getRandomQuote();

            playClosingSound();
            showNotification("Closing Time! " + formattedTime,
                    "Pack up your stuff, it's time to leave DepEd NCR!\n" + formattedDate,
                    NotificationCompat.PRIORITY_HIGH);

            logActivityToFile("Task executed at " + formattedTime + " on " + formattedDate);

            showMiniToast("Remember: " + randomQuote);

            triggerHiddenEasterEgg();

            // FIX 4: Added a 3-second delay so the worker stays alive just long enough
            // for your MediaPlayer sound to actually play before the system kills the background thread.
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {}

            return Result.success();
        }

        private boolean checkBattery() {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus == null) return true;
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int) ((level / (float) scale) * 100);
            if (batteryPct < 10) {
                showNotification("Low Battery", "Skipped playing sound (Battery: " + batteryPct + "%)", NotificationCompat.PRIORITY_LOW);
                return false;
            }
            return true;
        }

        private void playClosingSound() {
            try {
                MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.dasound);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            } catch (Exception e) {
                showNotification("Playback Error", "Unable to play closing sound.", NotificationCompat.PRIORITY_DEFAULT);
            }
        }

        private void showNotification(String title, String message, int priority) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Closing Time", NotificationManager.IMPORTANCE_HIGH);
                manager.createNotificationChannel(channel);
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(priority)
                    .setAutoCancel(true);
            manager.notify(new Random().nextInt(10000), builder.build());
        }

        private void showMiniToast(String message) {
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show());
        }

        private void logActivityToFile(String logMessage) {
            try {
                java.io.File logFile = new java.io.File(context.getFilesDir(), "clank_log.txt");
                java.io.FileWriter writer = new java.io.FileWriter(logFile, true);
                writer.append(logMessage).append("\n");
                writer.close();
            } catch (Exception ignored) {}
        }

        private void triggerHiddenEasterEgg() {
            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);

            // FIX 3: Updated to 18 (6 PM) to match your new closing time!
            if (hour == 18 && minute == 0) {
                showNotification("Secret Bonus 🎉", "You discovered the exact closing time!", NotificationCompat.PRIORITY_HIGH);
            }

            if (hour == 4 && minute == 20) {
                showNotification("Night Owl Mode 🌙", "Why are you awake right now?", NotificationCompat.PRIORITY_LOW);
            }

            if (hour == 12 && minute == 34) {
                showNotification("1234 Time ✨", "Make a wish!", NotificationCompat.PRIORITY_DEFAULT);
            }
        }

        private String getRandomQuote() {
            String[] quotes = {
                    "Stay curious, bro!",
                    "Hard work beats talent when talent doesn’t work hard.",
                    "Keep coding, keep winning.",
                    "The grind never stops.",
                    "You’re doing great — trust the process!",
                    "Sometimes, the bug is actually a feature.",
                    "Drink water, touch grass, write code.",
                    "Rozi believes in you ❤️",
                    "Don’t just dream it — debug it.",
                    "Rozi loves you",
                    "Keep going man your dad Apollo will be proud",
                    "Thank you, Janda… you always make my day feel like lavender and coffee. - Rozi"
            };
            return quotes[new Random().nextInt(quotes.length)];
        }
    }