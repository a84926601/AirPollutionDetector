package com.yuntech.eb211.airpollutiondetector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

//使用時要將Service 注册到 AndroidManifest.xml Application 里面

public class BackgroundRefresher extends Service {
    private static final String TAG     = "BackgroundRefresher";
    private static final int delay      = 1800; // Delay between each search query in ms (15 min here)
    private final Handler mHandler      = new Handler();
    private Timer mTimer = null;
    private final List<Integer> notificationsFired  = new ArrayList<>();
    LocationProvider locationProvider;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        locationProvider=new LocationProvider(BackgroundRefresher.this);
        // Clear notifications fired array
        this.notificationsFired.clear();
        initializeTimer();
    }

    /**
     * Service destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
        locationProvider.destroyLocationManager();
        Toast.makeText(this, "服務已停止", Toast.LENGTH_SHORT).show();
    }

    private class TimeDisplay extends TimerTask {
        @Override
        public void run() {
            // Fetching data...
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(BackgroundRefresher.this, locationProvider.getLocation(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void retrieveAirQuality(final int identifier) {

        }
    }
    private void initializeTimer(){
        if (mTimer != null)
            mTimer.cancel();
        else
            mTimer = new Timer();
        // Launch refresher task service timer
        mTimer.scheduleAtFixedRate(new TimeDisplay(), 1000, delay);
    }
}
