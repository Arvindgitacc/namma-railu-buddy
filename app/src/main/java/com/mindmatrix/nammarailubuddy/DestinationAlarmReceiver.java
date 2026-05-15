package com.mindmatrix.nammarailubuddy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class DestinationAlarmReceiver extends BroadcastReceiver {
    static final String ACTION_DESTINATION_GEOFENCE = "com.mindmatrix.nammarailubuddy.DESTINATION_GEOFENCE";
    static final String EXTRA_DESTINATION_NAME = "destination_name";
    private static final String CHANNEL_ID = "destination_alarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null || event.hasError()) {
            return;
        }

        int transition = event.getGeofenceTransition();
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER
                || transition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            String destination = intent.getStringExtra(EXTRA_DESTINATION_NAME);
            if (destination == null || destination.trim().isEmpty()) {
                destination = "your destination";
            }
            showAlarm(context, destination);
        }
    }

    private void showAlarm(Context context, String destination) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Destination alarms",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts passengers when they are within 5 km of the selected station.");
            manager.createNotificationChannel(channel);
        }

        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent openApp = PendingIntent.getActivity(
                context,
                2001,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle("Destination nearby")
                .setContentText("You are within 5 km of " + destination + ".")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openApp);

        manager.notify(5001, builder.build());
    }
}
