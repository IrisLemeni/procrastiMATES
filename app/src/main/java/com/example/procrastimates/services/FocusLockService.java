package com.example.procrastimates.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.procrastimates.activities.MainActivity;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class FocusLockService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "FocusLockServiceChannel";
    private static final String CHANNEL_NAME = "Focus Lock Notifications";
    private static final String TAG = "FocusLockService";
    private static final long CHECK_INTERVAL = 1000; // Check every second
    public static final String ACTION_INTERRUPTION = "com.example.procrastimates.FOCUS_INTERRUPTED";
    public static final String ACTION_LOCK_STATE_CHANGED = "com.example.procrastimates.LOCK_STATE_CHANGED";
    public static final String EXTRA_LOCK_ACTIVE = "lock_active";

    private boolean isLockActive = false;
    private Handler handler;
    private Runnable checkForegroundAppRunnable;
    private long startTime;
    private long totalTimeOutside = 0;
    private String lastForegroundApp = "";

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        checkForegroundAppRunnable = new Runnable() {
            @Override
            public void run() {
                if (isLockActive) {
                    try {
                        String currentApp = getForegroundApp();
                        Log.d(TAG, "=== FOCUS CHECK ===");
                        Log.d(TAG, "Current app (detected): " + currentApp);
                        Log.d(TAG, "Last known app (service state): " + lastForegroundApp);
                        Log.d(TAG, "Our package: com.example.procrastimates");

                        if (currentApp == null) {
                            Log.w(TAG, "Couldn't determine foreground app - permission issue or error.");
                            // Keep lastForegroundApp as is if we can't determine current
                            handler.postDelayed(this, CHECK_INTERVAL);
                            return;
                        }

                        boolean isOurApp = currentApp.contains("com.example.procrastimates");
                        boolean wasOurAppLast = lastForegroundApp.contains("com.example.procrastimates");

                        Log.d(TAG, "Is our app (current): " + isOurApp + ", Was our app (last): " + wasOurAppLast);

                        if (!isOurApp) {
                            // Current app is NOT our app
                            Log.d(TAG, "User is outside our app: " + currentApp);

                            totalTimeOutside += CHECK_INTERVAL;

                            // Only send interruption if we just left our app OR if the app outside is different from last time
                            // This ensures we always report the current app if it's external, and accumulate time.
                            if (wasOurAppLast || !currentApp.equals(lastForegroundApp)) {
                                Intent interruptionIntent = new Intent(ACTION_INTERRUPTION);
                                interruptionIntent.putExtra("app_name", currentApp);
                                interruptionIntent.putExtra("time_outside", totalTimeOutside); // Send accumulated time
                                LocalBroadcastManager.getInstance(FocusLockService.this)
                                        .sendBroadcast(interruptionIntent);

                                bringBackToApp();
                                Log.d(TAG, "FOCUS LOST - Interruption broadcast sent and bringing back to app. Current external app: " + currentApp);
                            } else {
                                // Still outside, in the same external app. Accumulating time.
                                // No new broadcast/bringBackToApp unless you want to spam it
                                Log.d(TAG, "Still outside in same app: " + currentApp + ". Total time outside: " + totalTimeOutside + "ms");
                            }
                        } else {
                            // Current app IS our app
                            if (!wasOurAppLast) {
                                // Just returned to our app from an external one
                                Log.d(TAG, "FOCUS REGAINED - User returned to our app.");
                                totalTimeOutside = 0; // Reset total time outside
                            }
                        }
                        lastForegroundApp = currentApp; // Always update lastForegroundApp
                    } catch (SecurityException e) {
                        Log.e(TAG, "Permission denied for usage stats", e);
                        setLockActive(false);
                        return;
                    } catch (Exception e) {
                        Log.e(TAG, "Error in focus check", e);
                    }
                }
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(EXTRA_LOCK_ACTIVE)) {
            boolean shouldBeActive = intent.getBooleanExtra(EXTRA_LOCK_ACTIVE, false);
            setLockActive(shouldBeActive);

            if (isLockActive) {
                startTime = System.currentTimeMillis();
                totalTimeOutside = 0;
                lastForegroundApp = getForegroundApp();

                // Start as foreground service
                Notification notification = buildForegroundNotification();
                startForeground(NOTIFICATION_ID, notification);

                // Start checking foreground app
                handler.post(checkForegroundAppRunnable);
            } else {
                // Stop checking
                handler.removeCallbacks(checkForegroundAppRunnable);
                stopForeground(true); // Remove notification and stop service from foreground
                stopSelf(); // Stop the service itself if it's no longer needed
            }
        }

        return START_STICKY; // Or START_NOT_STICKY if you don't want it to restart
    }
    private void createNotificationChannel() {
        // Only create the channel on Android O (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) { // Null check for safety
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification buildForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        // Use FLAG_IMMUTABLE for PendingIntent creation for API 23+ (and required for S+)
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;

        // Use the appropriate Notification.Builder constructor based on API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("Focus Lock Active")
                .setContentText("Keeping you focused on your tasks.")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app icon
                .setContentIntent(pendingIntent)
                .setOngoing(true); // Makes the notification non-dismissible

        // Category is available from API 21 (Lollipop)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_SERVICE);
        }

        return builder.build();
    }


    private void setLockActive(boolean active) {
        isLockActive = active;

        // Broadcast lock state change
        Intent stateIntent = new Intent(ACTION_LOCK_STATE_CHANGED);
        stateIntent.putExtra(EXTRA_LOCK_ACTIVE, isLockActive);
        LocalBroadcastManager.getInstance(this).sendBroadcast(stateIntent);
    }

    // Înlocuiește getForegroundApp() cu această versiune îmbunătățită:
    private String getForegroundApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
                if (usm == null) {
                    Log.e(TAG, "UsageStatsManager is null!");
                    return null;
                }

                long time = System.currentTimeMillis();
                // Mărește intervalul pentru emulator
                long queryInterval = TimeUnit.SECONDS.toMillis(30); // 30 secunde în loc de 10

                Log.d(TAG, "Querying usage events from " + (time - queryInterval) + " to " + time);

                UsageEvents usageEvents = usm.queryEvents(time - queryInterval, time);
                UsageEvents.Event event = new UsageEvents.Event();
                String currentApp = null;
                long latestTimestamp = 0;
                int eventCount = 0;

                // Găsește cel mai recent eveniment MOVE_TO_FOREGROUND
                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event);
                    eventCount++;
                    Log.d(TAG, "Event: Type=" + event.getEventType() +
                            ", Package=" + event.getPackageName() +
                            ", Timestamp=" + event.getTimeStamp());

                    if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND &&
                            event.getTimeStamp() > latestTimestamp) {
                        currentApp = event.getPackageName();
                        latestTimestamp = event.getTimeStamp();
                        Log.d(TAG, "New foreground app found: " + currentApp + " at " + latestTimestamp);
                    }
                }

                Log.d(TAG, "Total events processed: " + eventCount + ", Final app: " + currentApp);
                return currentApp != null ? currentApp : "";

            } catch (SecurityException e) {
                Log.e(TAG, "Usage stats access denied: " + e.getMessage());
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Error in getForegroundApp(): " + e.getMessage(), e);
                return null;
            }
        }

        Log.w(TAG, "Usage stats not supported below API 21.");
        return "";
    }

    private void bringBackToApp() {
        // Create an intent to bring back to our app
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cleanup
        handler.removeCallbacks(checkForegroundAppRunnable);
        isLockActive = false;
    }

    // Check if the app has usage stats permission
    public static boolean hasUsageStatsPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) {
                return false;
            }

            try {
                long time = System.currentTimeMillis();
                // Încearcă să obții statistici din ultimele 10 secunde
                UsageEvents usageEvents = usm.queryEvents(time - 10000, time);

                // Dacă poți obține evenimente, înseamnă că ai permisiunea
                if (usageEvents != null) {
                    // Încearcă să parcurgi evenimentele pentru a fi sigur
                    UsageEvents.Event event = new UsageEvents.Event();
                    usageEvents.hasNextEvent(); // Testează accesul real
                    return true;
                }
                return false;
            } catch (SecurityException e) {
                Log.e(TAG, "Usage stats permission not granted", e);
                return false;
            } catch (Exception e) {
                Log.e(TAG, "Error checking usage stats permission", e);
                return false;
            }
        }
        return false;
    }

    // Get intent to open usage stats settings
    public static Intent getUsageStatsSettingsIntent() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        return intent;
    }
}