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
    private static final int delay      = 1800000; // Delay between each search query in ms (15 min here)
    private final Handler mHandler      = new Handler();
    private LocationManager mLocationManager = null;
    private Timer mTimer = null;
    Location mLastLocation;
    private final List<Integer> notificationsFired  = new ArrayList<>();
    private class LocationListener implements android.location.LocationListener{
        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }
        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
        }
        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }
        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }
    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        initializeLocationManager();
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
        destroyLocationManager();
        Toast.makeText(this, "服務已停止", Toast.LENGTH_SHORT).show();
    }

    private class TimeDisplay extends TimerTask {
        @Override
        public void run() {
            // Fetching data...
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    getLocation();
                    showLocation();
                }
            });
        }

        private void retrieveAirQuality(final int identifier) {

        }

        private void getLocation() {
            for (int i = mLocationListeners.length - 1; i >= 0; i--) {   //順序決定精準度
                try {
                    mLocationManager.requestSingleUpdate(
                            LocationManager.NETWORK_PROVIDER, mLocationListeners[i], null);
                } catch (java.lang.SecurityException ex) {
                    Log.i(TAG, "fail to request location update, ignore", ex);
                } catch (IllegalArgumentException ex) {
                    Log.d(TAG, "network provider does not exist, " + ex.getMessage());
                }
            }
        }
        private void showLocation() {
            String Address;
            try{
                Address=getAddress(mLastLocation);
            }catch (IOException e){
                Log.e(TAG,"未初始化Location");
                return;
            }
            Toast.makeText(BackgroundRefresher.this, Address, Toast.LENGTH_LONG).show();
        }
        private String getAddress(Location location) throws IOException {
            Geocoder geocoder = new Geocoder(BackgroundRefresher.this);
            boolean falg = geocoder.isPresent();
            Log.e("xjp", "the falg is " + falg);
            StringBuilder stringBuilder = new StringBuilder();
            try {
                //根据经纬度获取地理位置信息
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                return stringBuilder.toString();
            }catch (IOException e) {
                // TODO Auto-generated catch block
                Toast.makeText(BackgroundRefresher.this, "位置解析錯誤", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            return null;
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
    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }
    private void destroyLocationManager(){
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }
}
