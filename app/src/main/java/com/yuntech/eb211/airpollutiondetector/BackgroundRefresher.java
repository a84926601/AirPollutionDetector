package com.yuntech.eb211.airpollutiondetector;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
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
    DataProvider dataProvider;

    public class LocalBinder extends Binder {
        public BackgroundRefresher getService() {
            return BackgroundRefresher.this;
        }
    }
    private IBinder binder = new LocalBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        locationProvider=new LocationProvider(BackgroundRefresher.this);
        dataProvider=new DataProvider(locationProvider);
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
                    dataProvider.getNearestStation();
                }
            });
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
