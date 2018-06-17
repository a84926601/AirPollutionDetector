package com.yuntech.eb211.airpollutiondetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.max;

//使用時要將Service 注册到 AndroidManifest.xml Application 里面

public class BackgroundRefresher extends Service {
    private static final String TAG     = "BackgroundRefresher";
    private static final int delay      = 60000; // Delay between each search query in ms (15 min here)
    private final Handler mHandler      = new Handler();
    private Timer mTimer = null,heartbeat=null;
    private final List<Integer> notificationsFired  = new ArrayList<>();
    LocationProvider locationProvider;
    DataProvider dataProvider;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        locationProvider=new LocationProvider(BackgroundRefresher.this);
        dataProvider=new DataProvider(locationProvider);
        // Clear notifications fired array
        this.notificationsFired.clear();
        initializeTimer();
    }
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        //這裡實作你想做的工作
        return Service.START_STICKY;
    }
    /**
     * Service destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
        locationProvider.destroyLocationManager();
        Log.e(TAG,"Service Stopped");
        Toast.makeText(this, "服務已停止", Toast.LENGTH_SHORT).show();
    }

    private class TimeDisplay extends TimerTask {
        @Override
        public void run() {
            // Fetching data...
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    dataProvider.getNearestStation(null,BackgroundRefresher.this);
                    startService(new Intent(BackgroundRefresher.this,BackgroundRefresher.class));
                }
            });
        }
    }
    private class Heartbeat extends TimerTask {
        @Override
        public void run() {
            // Fetching data...
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    startService(new Intent(BackgroundRefresher.this,BackgroundRefresher.class));
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
        //保持Service不停止
        if (heartbeat != null)
            heartbeat.cancel();
        else
            heartbeat = new Timer();
        // Launch refresher task service timer
        heartbeat.scheduleAtFixedRate(new Heartbeat(), 1000, 3000);
    }
    public void sendAlertPushNotification(String city, int threshold) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default",
                    "YOUR_CHANNEL_NAME",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("YOUR_NOTIFICATION_CHANNEL_DISCRIPTION");
            mNotificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder b = new NotificationCompat.Builder(BackgroundRefresher.this,"default");
        Random ran = new Random();
        Integer pushIdentifier = (int) System.currentTimeMillis() + (5 + ran.nextInt(max - 5 + 1));

        Intent notificationIntent = new Intent(BackgroundRefresher.this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(BackgroundRefresher.this, 0, notificationIntent, 0);

        b.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("空氣汙染警報")
                .setContentText(city+"空氣品質不良 AQI"+threshold)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setContentIntent(contentIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, b.build());
    }
}
