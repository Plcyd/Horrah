package com.placyd.horrah;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import androidx.core.app.NotificationCompat;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, Horrah.class);
            serviceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(serviceIntent);

            new Horrah().onCreate();

            IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, batteryIntentFilter);
            int level = 0;
            int scale = 100;
            if (batteryStatus != null) {
                level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            }
            int batteryPct = (int) ((level / (float) scale) * 100);

            String channelId = "boot_battery_channel";
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(channelId, "Startup Info", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                    .setContentTitle("Device Booted")
                    .setContentText("Battery at startup: " + batteryPct + "%")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true);

            manager.notify(2001, builder.build());
        }
    }
}
