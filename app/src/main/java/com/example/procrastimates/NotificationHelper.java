package com.example.procrastimates;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.procrastimates.R;
import com.example.procrastimates.activities.MainActivity;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";

    // Use a consistent channel ID throughout the app
    public static final String CHANNEL_ID = "procrastimates_notifications";
    public static final String CHANNEL_NAME = "Procrastimates Notifications";
    public static final String CHANNEL_DESC = "Notificări pentru task-uri și obiecții";

    /**
     * Creates the notification channel for Android O and above
     * Call this in Application.onCreate() or when your service starts
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created: " + CHANNEL_ID);
            } else {
                Log.e(TAG, "Failed to get NotificationManager");
            }
        }
    }

    /**
     * Shows a notification with the given title and body
     */
    public static void showNotification(Context context, String title, String body, String taskId, String circleId) {
        Log.d(TAG, "Showing notification: " + title + " - " + body);

        // Create intent for notification click
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add deep link data if available
        if (taskId != null) {
            intent.putExtra("taskId", taskId);
            Log.d(TAG, "Adding taskId to intent: " + taskId);
        }
        if (circleId != null) {
            intent.putExtra("circleId", circleId);
            Log.d(TAG, "Adding circleId to intent: " + circleId);
        }

        // Create pending intent with unique request code to avoid overriding
        int requestCode = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Set notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Show notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Check permission before showing notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "POST_NOTIFICATIONS permission not granted");
                return;
            }
        }

        try {
            int notificationId = (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification sent with ID: " + notificationId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to show notification", e);
        }
    }

    /**
     * Debug method to send a test notification immediately
     */
    public static void sendTestNotification(Context context) {
        Log.d(TAG, "Sending test notification");
        showNotification(
                context,
                "Test Notification",
                "This is a test notification from Procrastimates",
                null,
                null
        );
    }
}