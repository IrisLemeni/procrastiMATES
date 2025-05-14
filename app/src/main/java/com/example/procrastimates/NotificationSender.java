package com.example.procrastimates;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NotificationSender {
    private static final String FCM_API = "https://fcm.googleapis.com/fcm/send";
    private static final String REMOTE_CONFIG_KEY_FCM = "fcm_server_key";

    public static void sendPushNotification(String userId, String title, String body) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String deviceToken = documentSnapshot.getString("deviceToken");
                        if (deviceToken != null && !deviceToken.isEmpty()) {
                            fetchFcmServerKeyAndSendNotification(deviceToken, title, body);
                        }
                    }
                });
    }

    private static void fetchFcmServerKeyAndSendNotification(String deviceToken, String title, String body) {
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();

        // Configurări pentru Remote Config (opțional)
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // Interval de actualizare (1 oră)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);

        // Setează o valoare implicită (pentru cazul în care fetch-ul eșuează)
        remoteConfig.setDefaultsAsync(Map.of(REMOTE_CONFIG_KEY_FCM, ""));

        // Obține cheia FCM de la Remote Config
        remoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String serverKey = remoteConfig.getString(REMOTE_CONFIG_KEY_FCM);
                if (!serverKey.isEmpty()) {
                    sendNotificationToDevice(deviceToken, title, body, serverKey);
                } else {
                    Log.e("NotificationSender", "FCM Server Key is empty in Remote Config");
                }
            } else {
                Log.e("NotificationSender", "Failed to fetch FCM Server Key", task.getException());
            }
        });
    }

    private static void sendNotificationToDevice(String deviceToken, String title, String body, String serverKey) {
        try {
            JSONObject notification = new JSONObject();
            JSONObject notificationBody = new JSONObject();

            notificationBody.put("title", title);
            notificationBody.put("body", body);
            notification.put("to", deviceToken);
            notification.put("notification", notificationBody);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, FCM_API, notification,
                    response -> Log.d("NotificationSender", "Success: " + response.toString()),
                    error -> Log.e("NotificationSender", "Error: " + error.toString())
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "key=" + serverKey); // Folosește cheia din Remote Config
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            RequestQueue requestQueue = Volley.newRequestQueue(MyApplication.getAppContext());
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e("NotificationSender", "JSON Exception: " + e.getMessage());
        }
    }
}
