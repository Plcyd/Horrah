package com.placyd.horrah;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaPlayer;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.Calendar;

@SuppressLint("WorkerHasAPublicModifier")
class ClankWorker extends Worker {

    public ClankWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) return Result.success();

        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.dasound);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);

        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "closing_time_channel";

        NotificationChannel channel = new NotificationChannel(channelId, "Closing Time", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Closing Time!")
                .setContentText("Pack up your stuff, it's time to leave DepEd NCR!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(1001, builder.build());

        return Result.success();
    }
}
