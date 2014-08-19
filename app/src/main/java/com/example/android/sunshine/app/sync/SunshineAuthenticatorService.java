package com.example.android.sunshine.app.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by dimitri on 20/08/14.
 */
public class SunshineAuthenticatorService extends Service {

    private SunshineAuthenticator sunshineAuthenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        sunshineAuthenticator = new SunshineAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sunshineAuthenticator.getIBinder();
    }
}
