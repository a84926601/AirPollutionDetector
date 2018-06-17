package com.yuntech.eb211.airpollutiondetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import java.util.Random;

import static android.R.attr.max;

//使用時要將Service 注册到 AndroidManifest.xml Application 里面

public class BackgroundRefresher extends JobService {
    private static final int jobId=12,delay=2;
    private static final String TAG     = "BackgroundRefresher";
    LocationProvider locationProvider;
    DataProvider dataProvider;

    @Override
    public boolean onStartJob(JobParameters params) {
        locationProvider=new LocationProvider(BackgroundRefresher.this);
        dataProvider=new DataProvider(locationProvider);
        // Clear notifications fired array
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                dataProvider.getNearestStation(null,BackgroundRefresher.this);
            }
        });
        ScheduleNextJob();
        jobFinished(params, false);
        return true;
    }
    /**
     * Service destroyed
     */
    @Override
    public boolean onStopJob(JobParameters params) {
        super.onDestroy();
        locationProvider.destroyLocationManager();
        Log.e(TAG,"Service Stopped");
        //Toast.makeText(this, "服務已停止", Toast.LENGTH_SHORT).show();
        return false;
    }
    private void ScheduleNextJob(){
        JobScheduler mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo.Builder builder = new JobInfo.Builder(jobId,
                new ComponentName(getPackageName(), BackgroundRefresher.class.getName()));
        builder.setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(delay*60000); //週期執行

        mJobScheduler.schedule(builder.build());
        Log.e(TAG,"NowHasBeenScheduled");
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
