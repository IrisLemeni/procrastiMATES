package com.example.procrastimates.service;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Aici primești notificarea
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            // Poți afișa o notificare locală aici
            Log.d("FCM", "Notificare primită: " + title + " - " + body);
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d("FCM", "Token nou: " + token);
        // Salvează din nou tokenul dacă s-a schimbat
    }
}
