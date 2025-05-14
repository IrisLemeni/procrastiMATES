package com.example.procrastimates;

import android.app.Application;
import android.content.Context;

import com.example.procrastimates.fragments.PollScheduler;

public class MyApplication extends Application {

    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        PollScheduler.schedulePollChecks();
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}
