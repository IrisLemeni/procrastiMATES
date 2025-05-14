package com.example.procrastimates.service;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.procrastimates.MyApplication;
import com.example.procrastimates.NotificationHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onCreate() {
        super.onCreate();
        // Create notification channel at service creation
        NotificationHelper.createNotificationChannel(this);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            Log.d(TAG, "Message Notification Title: " + title);
            Log.d(TAG, "Message Notification Body: " + body);

            // Get data payload if exists
            String taskId = null;
            String circleId = null;

            if (remoteMessage.getData().size() > 0) {
                Log.d(TAG, "Message data payload: " + remoteMessage.getData());
                taskId = remoteMessage.getData().get("taskId");
                circleId = remoteMessage.getData().get("circleId");
            }

            // Show notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
                    NotificationHelper.showNotification(this, title, body, taskId, circleId);
                } else {
                    Log.w(TAG, "Cannot display notification: Missing POST_NOTIFICATIONS permission");
                }
            } else {
                // For Android < 13 we don't need the POST_NOTIFICATIONS permission
                NotificationHelper.showNotification(this, title, body, taskId, circleId);
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Save the new token to Firestore if user is logged in
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            sendRegistrationToServer(token);
        }
    }

    private void sendRegistrationToServer(String token) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();

            // Update token in user document
            Map<String, Object> updates = new HashMap<>();
            updates.put("deviceToken", token);
            updates.put("tokenUpdatedAt", new Timestamp(new Date()));
            updates.put("platform", "android"); // Add platform info

            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Device token updated successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update device token", e);
                    });
        }
    }
}