package com.voltazor.myapplication;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by voltazor on 14/04/16.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}
