package com.example.procrastimates.service;

import android.app.ActivityManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
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

        checkForegroundAppRunnable = new Runnable() {
            @Override
            public void run() {
                if (isLockActive) {
                    String currentApp = getForegroundApp();

                    // If not our app and different from last detected app (to prevent multiple counts)
                    if (!currentApp.contains("com.example.procrastimates") &&
                            !currentApp.equals(lastForegroundApp)) {

                        // Record app switch
                        lastForegroundApp = currentApp;
                        totalTimeOutside += CHECK_INTERVAL;

                        // Send broadcast that focus was interrupted
                        Intent interruptionIntent = new Intent(ACTION_INTERRUPTION);
                        interruptionIntent.putExtra("app_name", currentApp);
                        interruptionIntent.putExtra("time_outside", totalTimeOutside);
                        LocalBroadcastManager.getInstance(FocusLockService.this)
                                .sendBroadcast(interruptionIntent);

                        // Bring back to our app
                        bringBackToApp();
                    } else if (currentApp.contains("com.example.procrastimates") &&
                            !lastForegroundApp.contains("com.example.procrastimates")) {
                        // User came back to our app
                        lastForegroundApp = currentApp;
                    }
                }

                // Schedule next check
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(EXTRA_LOCK_ACTIVE)) {
            setLockActive(intent.getBooleanExtra(EXTRA_LOCK_ACTIVE, false));

            if (isLockActive) {
                startTime = System.currentTimeMillis();
                totalTimeOutside = 0;
                lastForegroundApp = getForegroundApp();

                // Start checking foreground app
                handler.post(checkForegroundAppRunnable);
            } else {
                // Stop checking
                handler.removeCallbacks(checkForegroundAppRunnable);
            }
        }

        return START_STICKY;
    }

    private void setLockActive(boolean active) {
        isLockActive = active;

        // Broadcast lock state change
        Intent stateIntent = new Intent(ACTION_LOCK_STATE_CHANGED);
        stateIntent.putExtra(EXTRA_LOCK_ACTIVE, isLockActive);
        LocalBroadcastManager.getInstance(this).sendBroadcast(stateIntent);
    }

    private String getForegroundApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageEvents.Event> events = null;

            UsageEvents usageEvents = usm.queryEvents(time - 1000*3600, time);
            UsageEvents.Event event = new UsageEvents.Event();
            String currentApp = "";

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    currentApp = event.getPackageName();
                }
            }

            if (currentApp.isEmpty()) {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
                if (runningProcesses != null) {
                    for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                        if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            return processInfo.processName;
                        }
                    }
                }
            }
            return currentApp; // Return the detected app package name
        }
        return ""; // Return empty string if SDK version doesn't support usage stats
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
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageEvents.Event> events = null;
        UsageEvents usageEvents = usm.queryEvents(time - TimeUnit.MINUTES.toMillis(1), time);

        // If we can get events, we have the permission
        return usageEvents != null && usageEvents.hasNextEvent();
    }

    // Get intent to open usage stats settings
    public static Intent getUsageStatsSettingsIntent() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        return intent;
    }
}
