package com.placyd.horrah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;

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
            MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.dasound);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
        }

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

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Closing Time!")
                .setContentText("Pack up your stuff, it's time to leave GigaHertz!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 250, 500})
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPending)
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);

        // Show the notification
        manager.notify(1001, builder.build());
    }
}
