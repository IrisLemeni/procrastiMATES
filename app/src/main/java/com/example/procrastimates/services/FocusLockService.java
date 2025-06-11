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
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.procrastimates.R;
import com.example.procrastimates.activities.MainActivity;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class FocusLockService extends Service {

    private static final String TAG = "FocusLockService";
    private static final long CHECK_INTERVAL = 1500; // Reduced to 1.5 seconds for faster detection
    private static final String CHANNEL_ID = "focus_lock_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_INTERRUPTION = "com.example.procrastimates.FOCUS_INTERRUPTED";
    public static final String ACTION_LOCK_STATE_CHANGED = "com.example.procrastimates.LOCK_STATE_CHANGED";
    public static final String EXTRA_LOCK_ACTIVE = "lock_active";

    private boolean isLockActive = false;
    private Handler handler;
    private Runnable checkForegroundAppRunnable;
    private long startTime;
    private int totalInterruptions = 0;
    private String lastForegroundApp = "";
    private long lastCheckTime = 0;
    private boolean wasInOurApp = true;
    private PowerManager.WakeLock wakeLock;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

        // Acquire wake lock to prevent service from being killed
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ProcrastiMates:FocusLock");

        checkForegroundAppRunnable = new Runnable() {
            @Override
            public void run() {
                if (isLockActive) {
                    checkAndHandleForegroundApp();
                }
                // Schedule next check
                if (isLockActive) {
                    handler.postDelayed(this, CHECK_INTERVAL);
                }
            }
        };

        Log.d(TAG, "FocusLockService created");
    }

    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public FocusLockService getService() {
            return FocusLockService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");

        if (intent != null && intent.hasExtra(EXTRA_LOCK_ACTIVE)) {
            boolean shouldActivate = intent.getBooleanExtra(EXTRA_LOCK_ACTIVE, false);
            Log.d(TAG, "Lock should be active: " + shouldActivate);

            if (shouldActivate && !isLockActive) {
                startFocusLock();
            } else if (!shouldActivate && isLockActive) {
                stopFocusLock();
            }
        }

        return START_STICKY; // Restart if killed
    }

    private void startFocusLock() {
        Log.d(TAG, "Starting focus lock");
        isLockActive = true;
        startTime = System.currentTimeMillis();
        totalInterruptions = 0;
        lastForegroundApp = getForegroundApp();
        lastCheckTime = System.currentTimeMillis();
        wasInOurApp = lastForegroundApp.contains("com.example.procrastimates");

        // Acquire wake lock
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(10*60*1000L /*10 minutes*/);
        }

        // Start as foreground service with updated notification
        Notification notification = createFocusNotification();
        startForeground(NOTIFICATION_ID, notification);

        // Update notification to show active state
        updateNotification("Focus Session Active - Stay focused!");

        // Start checking
        handler.post(checkForegroundAppRunnable);

        // Broadcast state change
        broadcastLockStateChange(true);

        Log.d(TAG, "Focus lock started successfully");
    }

    private void stopFocusLock() {
        Log.d(TAG, "Stopping focus lock");
        isLockActive = false;

        // Release wake lock
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Stop foreground service
        stopForeground(true);

        // Stop checking
        handler.removeCallbacks(checkForegroundAppRunnable);

        // Broadcast state change
        broadcastLockStateChange(false);

        Log.d(TAG, "Focus lock stopped");
    }

    private void checkAndHandleForegroundApp() {
        String currentApp = getForegroundApp();
        long currentTime = System.currentTimeMillis();

        if (currentApp.isEmpty()) {
            Log.d(TAG, "Current app is empty, skipping check");
            return;
        }

        boolean isInOurApp = currentApp.contains("com.example.procrastimates");

        Log.d(TAG, "Current app: " + currentApp + ", Was in our app: " + wasInOurApp +
                ", Is in our app: " + isInOurApp + ", Last app: " + lastForegroundApp);

        // FIX: Detectare mai precisă a ieșirii din aplicație
        if (wasInOurApp && !isInOurApp && !currentApp.equals(lastForegroundApp)) {
            // Verifică că nu e o schimbare temporară (ex: notification panel)
            if (!isSystemApp(currentApp)) {
                totalInterruptions++;

                Log.d(TAG, "Focus interrupted by: " + currentApp + " (Interruption #" + totalInterruptions + ")");

                // Update notification to show interruption
                updateNotification("Focus interrupted! Tap to return (" + totalInterruptions + " interruptions)");

                // Send broadcast about interruption
                sendInterruptionBroadcast(currentApp, totalInterruptions, currentTime - startTime);

                // Try to bring back our app after a short delay
                handler.postDelayed(this::bringBackToApp, 1000); // Mărit delay-ul pentru stabilitate
            }
        } else if (!wasInOurApp && isInOurApp) {
            // User returned to our app
            updateNotification("Focus Session Active - Stay focused!");
            Log.d(TAG, "User returned to our app");
        }

        // Update tracking variables
        wasInOurApp = isInOurApp;
        lastForegroundApp = currentApp;
        lastCheckTime = currentTime;
    }

    private boolean isSystemApp(String packageName) {
        // Lista aplicațiilor de sistem care nu ar trebui să conteze ca întreruperi
        return packageName.contains("com.android.systemui") ||
                packageName.contains("com.samsung.android") ||
                packageName.contains("com.sec.android") ||
                packageName.equals("android") ||
                packageName.contains("launcher");
    }



    private void sendInterruptionBroadcast(String appName, int interruptionCount, long timeElapsed) {
        Intent interruptionIntent = new Intent(ACTION_INTERRUPTION);
        interruptionIntent.putExtra("app_name", appName);
        interruptionIntent.putExtra("interruption_count", interruptionCount);
        interruptionIntent.putExtra("time_elapsed", timeElapsed);

        // FIX: Trimite broadcast-ul sincron pe main thread
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            try {
                LocalBroadcastManager.getInstance(this).sendBroadcast(interruptionIntent);
                Log.d(TAG, "Interruption broadcast sent for app: " + appName +
                        ", count: " + interruptionCount + ", score should be: " +
                        Math.max(0, 100 - (interruptionCount * 10)));
            } catch (Exception e) {
                Log.e(TAG, "Error sending broadcast: " + e.getMessage());
            }
        });
    }

    private void updateNotification(String text) {
        if (notificationManager != null && isLockActive) {
            Notification notification = createFocusNotification(text);
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private String getForegroundApp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
                if (usm == null) {
                    Log.w(TAG, "UsageStatsManager is null");
                    return getForegroundAppFromRunningProcesses();
                }

                long currentTime = System.currentTimeMillis();

                // Query events from the last 3 seconds (reduced for faster detection)
                UsageEvents usageEvents = usm.queryEvents(currentTime - 3000, currentTime);
                if (usageEvents == null) {
                    Log.w(TAG, "UsageEvents is null");
                    return getForegroundAppFromRunningProcesses();
                }

                UsageEvents.Event event = new UsageEvents.Event();
                String currentApp = "";

                // Get the most recent MOVE_TO_FOREGROUND event
                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event);
                    if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        currentApp = event.getPackageName();
                    }
                }

                // If no recent events, try getting from running processes (fallback)
                if (currentApp.isEmpty()) {
                    currentApp = getForegroundAppFromRunningProcesses();
                }

                return currentApp;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting foreground app: " + e.getMessage());
        }

        return getForegroundAppFromRunningProcesses();
    }

    private String getForegroundAppFromRunningProcesses() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
                if (runningProcesses != null) {
                    for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                        if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            return processInfo.processName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting running processes: " + e.getMessage());
        }
        return "";
    }

    private void bringBackToApp() {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            Log.d(TAG, "Attempting to bring back to app");
        } catch (Exception e) {
            Log.e(TAG, "Error bringing back to app: " + e.getMessage());
        }
    }

    private void broadcastLockStateChange(boolean active) {
        Intent stateIntent = new Intent(ACTION_LOCK_STATE_CHANGED);
        stateIntent.putExtra(EXTRA_LOCK_ACTIVE, active);
        LocalBroadcastManager.getInstance(this).sendBroadcast(stateIntent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Focus Lock Service",
                    NotificationManager.IMPORTANCE_DEFAULT // Changed from LOW to DEFAULT for visibility
            );
            channel.setDescription("Monitors app focus during Pomodoro sessions");
            channel.setShowBadge(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 100});

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createFocusNotification() {
        return createFocusNotification("Focus Session Active - Stay focused!");
    }

    private Notification createFocusNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                        PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Use a default Android icon if custom icon doesn't exist
        int iconResource;
        try {
            iconResource = R.drawable.ic_notification;
        } catch (Exception e) {
            iconResource = android.R.drawable.ic_dialog_info; // Fallback to system icon
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ProcrastiMates Focus Lock")
                .setContentText(contentText)
                .setSmallIcon(iconResource)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Changed from LOW
                .setShowWhen(true)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");

        handler.removeCallbacks(checkForegroundAppRunnable);
        isLockActive = false;
        stopForeground(true);

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Task removed, restarting service");
        // Restart the service when task is removed
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.putExtra(EXTRA_LOCK_ACTIVE, isLockActive);
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    // Utility methods
    public static boolean hasUsageStatsPermission(Context context) {
        try {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return false;

            long currentTime = System.currentTimeMillis();
            UsageEvents usageEvents = usm.queryEvents(currentTime - TimeUnit.MINUTES.toMillis(1), currentTime);

            return usageEvents != null && usageEvents.hasNextEvent();
        } catch (Exception e) {
            Log.e(TAG, "Error checking usage stats permission: " + e.getMessage());
            return false;
        }
    }

    public static Intent getUsageStatsSettingsIntent() {
        return new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
    }

    public int getTotalInterruptions() {
        return totalInterruptions;
    }
    public int getCurrentFocusScore() {
        return Math.max(0, 100 - (totalInterruptions * 10));
    }

}