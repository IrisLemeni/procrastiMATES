package com.example.procrastimates.services;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.procrastimates.MyApplication;
import com.example.procrastimates.R;
import com.example.procrastimates.enums.NotificationType;
import com.example.procrastimates.utils.NotificationHelper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationSender {
    private static final String TAG = "NotificationSender";
    private static final String FCM_API = "https://fcm.googleapis.com/v1/projects/procrastimate-cc91b/messages:send";
    private static final List<String> SCOPES = Arrays.asList("https://www.googleapis.com/auth/firebase.messaging");
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static void sendPushNotification(String userId, String title, String body,
                                            String taskId, String circleId,
                                            NotificationType type) {
        // Log pentru debugging
        Log.d(TAG, "Starting notification process for user: " + userId);
        Log.d(TAG, "Title: " + title + ", Body: " + body);

        // Check if it's for the current user - if so, we can show directly
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        boolean isForCurrentUser = (currentUserId != null && currentUserId.equals(userId));

        // Create notification object for Firestore
        String notificationId = UUID.randomUUID().toString();
        Map<String, Object> notification = new HashMap<>();
        notification.put("notificationId", notificationId);
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("body", body);
        notification.put("taskId", taskId != null ? taskId : "");
        notification.put("circleId", circleId != null ? circleId : "");
        notification.put("type", type.toString());
        notification.put("read", false);
        notification.put("createdAt", new Timestamp(new Date()));

        // Save to Firestore first
        db.collection("notifications").document(notificationId)
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification saved to Firestore with ID: " + notificationId);

                    // If this is for the current user, show notification locally immediately
                    if (isForCurrentUser) {
                        if (MyApplication.getAppContext() != null) {
                            // Check permission before showing notification
                            boolean canShowNotification = true;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                canShowNotification = MyApplication.getAppContext().checkSelfPermission(
                                        Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
                            }

                            if (canShowNotification) {
                                Log.d(TAG, "Showing notification locally for current user");
                                NotificationHelper.showNotification(
                                        MyApplication.getAppContext(),
                                        title,
                                        body,
                                        taskId,
                                        circleId);
                            } else {
                                Log.e(TAG, "Cannot show notification - permission not granted");
                            }
                        } else {
                            Log.e(TAG, "Cannot show notification - app context is null");
                        }
                    }

                    // Get the user's device token and send FCM message for all cases
                    fetchUserAndSendFCM(userId, title, body, taskId, circleId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save notification to Firestore", e);
                });
    }

    private static void fetchUserAndSendFCM(String userId, String title, String body, String taskId, String circleId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Check for device token and also log other useful info
                        String deviceToken = documentSnapshot.getString("deviceToken");
                        String userEmail = documentSnapshot.getString("email");
                        String fcmEnabled = documentSnapshot.getString("fcmEnabled");

                        Log.d(TAG, "User info - Email: " + userEmail +
                                ", FCM Enabled: " + fcmEnabled +
                                ", Device Token: " + (deviceToken != null ? "present" : "missing"));

                        if (deviceToken != null && !deviceToken.isEmpty()) {
                            // Add debugging to check token format
                            if (deviceToken.length() < 50) {
                                Log.e(TAG, "Device token seems too short: " + deviceToken.length() + " chars");
                            }

                            // Send the actual notification
                            sendNotificationWithOAuth(deviceToken, title, body, taskId, circleId);
                        } else {
                            Log.w(TAG, "User does not have a valid device token");
                        }
                    } else {
                        Log.w(TAG, "User document does not exist for ID: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user device token", e);
                });
    }

    private static void sendNotificationWithOAuth(String deviceToken, String title, String body, String taskId, String circleId) {
        Log.d(TAG, "Attempting to send FCM notification with OAuth");

        // Verifică dacă avem access la contextul aplicației
        if (MyApplication.getAppContext() == null) {
            Log.e(TAG, "Application context is null");
            return;
        }

        // Utilizăm executorService pentru a executa operațiunile de rețea pe un thread secundar
        executorService.execute(() -> {
            try {
                // Încărcarea cheii contului de serviciu
                InputStream serviceAccount = null;
                try {
                    serviceAccount = MyApplication.getAppContext().getResources().openRawResource(R.raw.service_account);
                    Log.d(TAG, "Service account file loaded successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load service account file", e);
                    return;
                }

                // Obținerea credențialelor OAuth
                GoogleCredentials credentials = null;
                try {
                    credentials = GoogleCredentials.fromStream(serviceAccount)
                            .createScoped(SCOPES);
                    Log.d(TAG, "Credentials created successfully");
                } catch (IOException e) {
                    Log.e(TAG, "Failed to create credentials from service account", e);
                    return;
                } finally {
                    try {
                        if (serviceAccount != null) {
                            serviceAccount.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to close service account input stream", e);
                    }
                }

                // Reîmprospătarea token-ului
                try {
                    credentials.refreshIfExpired();
                    Log.d(TAG, "Credentials refreshed successfully");
                } catch (IOException e) {
                    Log.e(TAG, "Failed to refresh credentials", e);
                    return;
                }

                String accessToken = credentials.getAccessToken().getTokenValue();
                if (accessToken == null || accessToken.isEmpty()) {
                    Log.e(TAG, "Access token is null or empty");
                    return;
                }
                Log.d(TAG, "Access token obtained successfully");

                // Construcția payload-ului v1
                JSONObject message = new JSONObject();
                JSONObject notification = new JSONObject();
                JSONObject data = new JSONObject();
                JSONObject root = new JSONObject();

                try {
                    notification.put("title", title);
                    notification.put("body", body);

                    if (taskId != null) data.put("taskId", taskId);
                    if (circleId != null) data.put("circleId", circleId);

                    // Add an additional flag to help tracking
                    data.put("source", "procrastimates_app");

                    message.put("token", deviceToken);
                    message.put("notification", notification);
                    message.put("data", data);

                    root.put("message", message);

                    Log.d(TAG, "FCM payload created: " + root.toString());
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to build JSON payload", e);
                    return;
                }

                // Crearea și trimiterea cererii HTTP
                final JSONObject finalRoot = root;

                // Trebuie să rulăm Volley pe UI thread, dar restul operațiunilor pe thread secundar
                android.os.Handler mainHandler = new android.os.Handler(MyApplication.getAppContext().getMainLooper());
                mainHandler.post(() -> {
                    JsonObjectRequest request = new JsonObjectRequest(
                            Request.Method.POST, FCM_API, finalRoot,
                            response -> Log.d(TAG, "FCM notification sent successfully: " + response),
                            error -> {
                                Log.e(TAG, "FCM error: " + error.toString());
                                if (error.networkResponse != null) {
                                    Log.e(TAG, "Error status code: " + error.networkResponse.statusCode);
                                    try {
                                        String responseBody = new String(error.networkResponse.data, "utf-8");
                                        Log.e(TAG, "Error response: " + responseBody);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Could not parse error response", e);
                                    }
                                }
                            }
                    ) {
                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Authorization", "Bearer " + accessToken);
                            headers.put("Content-Type", "application/json; charset=UTF-8");
                            return headers;
                        }
                    };

                    // Setăm politica de reîncercare și timeout
                    request.setRetryPolicy(new DefaultRetryPolicy(
                            30000, // 30 secunde timeout
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                    // Adăugarea cererii în coada de cereri
                    try {
                        RequestQueue queue = Volley.newRequestQueue(MyApplication.getAppContext());
                        queue.add(request);
                        Log.d(TAG, "FCM request added to queue");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to add request to queue", e);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "OAuth Notification Error", e);
                // Log stiva de apeluri pentru a vedea unde apare eroarea
                e.printStackTrace();
            }
        });
    }


}