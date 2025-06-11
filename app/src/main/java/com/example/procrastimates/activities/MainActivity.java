package com.example.procrastimates.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.procrastimates.services.NotificationSender;
import com.example.procrastimates.enums.NotificationType;
import com.example.procrastimates.R;
import com.example.procrastimates.fragments.FriendsFragment;
import com.example.procrastimates.fragments.HomeFragment;
import com.example.procrastimates.fragments.PomodoroFragment;
import com.example.procrastimates.fragments.TasksFragment;
import com.example.procrastimates.utils.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements PomodoroFragment.FocusLockListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Load the default fragment
        loadFragment(new HomeFragment());

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    loadFragment(new HomeFragment());

                } else if (itemId == R.id.nav_tasks) {
                    loadFragment(new TasksFragment());

                } else if (itemId == R.id.nav_pomodoro) {
                    loadFragment(new PomodoroFragment());

                } else if (itemId == R.id.nav_friends) {
                    loadFragment(new FriendsFragment());
                }

                return true;
            }
        });

        // Setup for notification functionality
        setupNotifications();

        // Check and request notification permissions
        requestNotificationPermissionIfNeeded();

        // Check if we were opened from a notification
        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            Bundle extras = intent.getExtras();
            String taskId = extras.getString("taskId");
            String circleId = extras.getString("circleId");
            String notificationType = extras.getString("notificationType");

            if (circleId != null) {
                Log.d(TAG, "Opened from circle notification with circleId: " + circleId);

                // Deschide direct CircleChatActivity
                Intent chatIntent = new Intent(this, CircleChatActivity.class);
                chatIntent.putExtra("circleId", circleId);

                // Adaugă flag-uri pentru a curăța back stack-ul dacă e necesar
                chatIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(chatIntent);

            } else if (taskId != null) {
                Log.d(TAG, "Opened from task notification with taskId: " + taskId);
                // Păstrează logica existentă pentru notificările legate de task-uri
                loadFragment(new TasksFragment());
            }
        }
    }

    private void setupNotifications() {
        // Setup notification channel
        NotificationHelper.createNotificationChannel(this);

        // Request Firebase messaging token and save to Firestore
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d(TAG, "FCM Token obtained: " + token);

                        // Save token in Firestore if user is logged in
                        FirebaseAuth auth = FirebaseAuth.getInstance();
                        if (auth.getCurrentUser() != null) {
                            String userId = auth.getCurrentUser().getUid();

                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(userId)
                                    .update("deviceToken", token)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "FCM Token saved to Firestore");

                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "FCM Token save error: " + e.getMessage());
                                    });
                        }
                    } else {
                        Log.e(TAG, "FCM Token error: " + task.getException());
                    }
                });
    }

    private void requestNotificationPermissionIfNeeded() {
        // Check if we need to request notification permissions (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Check if we should show rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {

                    // Show an explanation dialog
                    new AlertDialog.Builder(this)
                            .setTitle("Notification Permission Required")
                            .setMessage("Procrastimates needs notification permission to alert you about " +
                                    "task deadlines and updates from your friends.")
                            .setPositiveButton("Grant Permission", (dialog, which) -> {
                                // Request the permission
                                ActivityCompat.requestPermissions(this,
                                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                        PERMISSION_REQUEST_CODE);
                            })
                            .setNegativeButton("Not Now", (dialog, which) -> {
                                dialog.dismiss();
                                Toast.makeText(this,
                                        "You won't receive important notifications",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .create()
                            .show();
                } else {
                    // No explanation needed, request the permission
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            PERMISSION_REQUEST_CODE);
                }
            } else {
                // Permission already granted, send a test notification
                // This could help debug notification issues
                Log.d(TAG, "POST_NOTIFICATIONS permission already granted");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");

                // Permission was granted
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();

                // Send a test notification to verify it works
                sendTestNotification();
            } else {
                Log.w(TAG, "Notification permission denied");

                // Permission denied by user
                // Check if "Don't ask again" was selected
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.POST_NOTIFICATIONS)) {
                        // User selected "Don't ask again", show settings dialog
                        new AlertDialog.Builder(this)
                                .setTitle("Notifications Disabled")
                                .setMessage("You won't receive important task updates. " +
                                        "You can enable notifications in settings.")
                                .setPositiveButton("Open Settings", (dialog, which) -> {
                                    // Open app notification settings
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                                    startActivity(intent);
                                })
                                .setNegativeButton("Not Now", (dialog, which) -> dialog.dismiss())
                                .create()
                                .show();
                    } else {
                        Toast.makeText(this,
                                "You won't receive important task notifications",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    /**
     * Send a test notification to verify permissions and setup
     */
    private void sendTestNotification() {
        // Create a button to test notification
        Snackbar snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                "Would you like to test notifications?",
                Snackbar.LENGTH_LONG);

        snackbar.setAction("Send Test", v -> {
            // Use our notification helper to send a direct notification
            NotificationHelper.sendTestNotification(this);

            // Also send through the FCM system
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            NotificationSender.sendPushNotification(
                    userId,
                    "Task Update",
                    "Your friend completed a task!",
                    null,
                    null,
                    NotificationType.TASK_COMPLETED);

            Toast.makeText(this,
                    "Test notification sent",
                    Toast.LENGTH_SHORT).show();
        });

        snackbar.show();
    }

    @Override
    public void setBottomNavEnabled(boolean enabled) {
        if (bottomNavigationView != null) {
            // Dezactivează navigarea făcând elementele neclicabile
            for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
                bottomNavigationView.getMenu().getItem(i).setEnabled(enabled);
            }

            // Opțional: schimbă transparența pentru feedback vizual
            bottomNavigationView.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .commit();
    }
}