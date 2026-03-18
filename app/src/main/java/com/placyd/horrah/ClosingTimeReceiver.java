package com.placyd.horrah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Random;

public class ClosingTimeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        // Get the current day of the week
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_WEEK);

        // Skip weekends and Tuesday
        if (day == Calendar.TUESDAY || day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
            return;
        }

        // Check if phone is not in silent mode before playing sound
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null && audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {

            // ==========================================
            // RANDOM SOUND SELECTOR LOGIC ADDED HERE
            // ==========================================
            int[] soundFiles = {
                    R.raw.dasound,
                    R.raw.dasound2,
                    R.raw.dasound3,
                    R.raw.dasound4,
                    R.raw.dasound5,
                    R.raw.dasound6,
                    R.raw.dasound7
            };

            // Pick a random sound from the array
            int randomSound = soundFiles[new Random().nextInt(soundFiles.length)];

            MediaPlayer mediaPlayer = MediaPlayer.create(context, randomSound);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
        }

        // ==========================================
        // AWESOME NEW FEATURE 1: FUN DYNAMIC QUOTES! (EXPANDED!)
        // ==========================================
        String[] funQuotes = {
                "Pack up your stuff, it's time to leave GigaHertz!", // Your classic original!
                "Mission Passed! Respect + Time to head home.",
                "Quezon City and Manila traffic awaits! Catch that QCbus.",
                "CyberSafe TVI development can wait until tomorrow!",
                "Time to log off and call Rozi!",
                "You survived! Go chill and blast some Green Day.",
                "Have lumpia for Dinner, bro.",
                "UWIAN NA PRE!",
                // --- NEW BONUS QUOTES ---
                "Time to grind some GTA: San Andreas, bro!",
                "Queue up some Undertale gameplay videos to unwind.",
                "Go blast some Deftones on the commute home."
        };
        String selectedQuote = funQuotes[new Random().nextInt(funQuotes.length)];

        // ==========================================
        // AWESOME NEW FEATURE 2: POP-UP ON-SCREEN CHEER!
        // ==========================================
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, "🎉 " + selectedQuote, Toast.LENGTH_LONG).show()
        );

        // Create and show notification
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "closing_time_channel";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Closing Time",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 250, 500});
            // NEW BONUS: Enable custom LED lights for the channel
            channel.enableLights(true);
            channel.setLightColor(Color.parseColor("#FF5722"));
            manager.createNotificationChannel(channel);
        }

        // Action for dismissing notification
        Intent dismissIntent = new Intent(context, ClosingTimeReceiver.class);
        PendingIntent dismissPending = PendingIntent.getBroadcast(
                context,
                0,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // ==========================================
        // AWESOME NEW FEATURE 3: EXTRA "LET'S GO" BUTTON
        // ==========================================
        Intent partyIntent = new Intent(context, ClosingTimeReceiver.class);
        PendingIntent partyPending = PendingIntent.getBroadcast(
                context,
                1, // Different request code so it doesn't overlap
                partyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // ==========================================
        // BONUS FEATURE 1: "CALL ROZI" ACTION BUTTON
        // ==========================================
        Intent roziIntent = new Intent(context, ClosingTimeReceiver.class); // Can be changed to ACTION_DIAL later
        roziIntent.setAction("com.placyd.horrah.ACTION_CALL_ROZI"); // Unique action
        PendingIntent roziPending = PendingIntent.getBroadcast(
                context,
                2, // Different request code
                roziIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_horrah_notif) // THE NEW ICON!
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher)) // Full color logo inside
                .setColor(Color.parseColor("#FF5722")) // Added a bold orange/red energetic tint
                .setContentTitle("Closing Time!")
                .setContentText(selectedQuote) // Uses the random quote generator
                .setStyle(new NotificationCompat.BigTextStyle().bigText(selectedQuote)) // Ensures long text doesn't get cut off
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 250, 500})
                // BONUS FEATURE 2 & 3: Stop watch & LED flasher
                .setUsesChronometer(true) // Shows an active timer of how long you've been off work
                .setLights(Color.parseColor("#FF5722"), 1000, 2000) // Hardware LED flashing pattern
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPending)
                .addAction(android.R.drawable.ic_media_play, "Let's Go!", partyPending) // The fun extra button
                .addAction(android.R.drawable.ic_menu_call, "Call Rozi", roziPending) // The new partner action button
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);

        // Show the notification
        manager.notify(1001, builder.build());
    }
}