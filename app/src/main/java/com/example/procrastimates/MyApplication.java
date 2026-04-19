package com.example.procrastimates;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.Manifest;

import androidx.core.content.ContextCompat;

import com.example.procrastimates.services.PollScheduler;
import com.example.procrastimates.utils.NotificationHelper;

public class MyApplication extends Application {
    private static final String TAG = "ProcrastimatesApp";
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        // Create notification channel early in app lifecycle
        NotificationHelper.createNotificationChannel(this);

        // Log notification permission status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Notification permission status: " +
                    (hasPermission ? "GRANTED" : "NOT GRANTED"));
        }
        PollScheduler.schedulePollChecks(this);

        // Log that app has started
        Log.d(TAG, "Application started");
    }

    public static Context getAppContext() {
        return context;
    }
}
