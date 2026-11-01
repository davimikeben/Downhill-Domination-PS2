package com.downhill.mobile;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

/** Application entry point: keeps a single prefs handle and pre-warms the pad engine. */
public class DownhillApp extends Application {

    private static DownhillApp instance;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = getSharedPreferences("downhill_pad", Context.MODE_PRIVATE);
    }

    public static DownhillApp get() {
        return instance;
    }

    public SharedPreferences prefs() {
        return prefs != null ? prefs : (prefs = getSharedPreferences("downhill_pad", MODE_PRIVATE));
    }
}
