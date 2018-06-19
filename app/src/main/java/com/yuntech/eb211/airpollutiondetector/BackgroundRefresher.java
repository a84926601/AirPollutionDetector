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
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import java.util.Random;

import static android.R.attr.max;

//使用時要將Service 注册到 AndroidManifest.xml Application 里面

public class BackgroundRefresher extends JobService {
    private boolean AlarmReceiver=false;
    private static final int jobId=12,delay=15;
    private static final String TAG = "BackgroundRefresher",CHANNEL_DEFAULT="default",CHANNEL_SILENT="silent",CHANNEL_VIBRATE="vibrate",CHANNEL_RING="ring";
    private SharedPreferences Setting;
    LocationProvider locationProvider;
    DataProvider dataProvider;

    @Override
    public boolean onStartJob(JobParameters params) {
        Setting = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        locationProvider=new LocationProvider(BackgroundRefresher.this);
        dataProvider=new DataProvider(locationProvider);
        // Clear notifications fired array
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                dataProvider.getNearestStation(null,BackgroundRefresher.this);
            }
        });
        if(params.getJobId()==jobId) ScheduleNextJob();
        else AlarmReceiver=true;
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
        String channelName=null;
        int minAlertNum=2;
        boolean vibrate=Setting.getBoolean(getString(R.string.key_enable_vibrate),true),
                ring=Setting.getBoolean(getString(R.string.key_enable_sound),true);
        switch (Setting.getString(getString(R.string.key_notify_limit),"2")){
            case "0" : minAlertNum=0; break;
            case "1" : minAlertNum=51;  break;
            case "2" : minAlertNum=101; break;
            case "3" : minAlertNum=151; break;
            case "4" : minAlertNum=201; break;
            case "5" : minAlertNum=301; break;
        }
        if(threshold>minAlertNum||AlarmReceiver){
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel;
                //鈴聲設定(8.0 up)
                if(ring&&vibrate){
                    channelName=CHANNEL_DEFAULT;
                    channel=iniChannel(channelName);
                    channel.enableVibration(true);
                }else if(ring){
                    channelName=CHANNEL_RING;
                    channel=iniChannel(channelName);
                    channel.setVibrationPattern(new long[]{0});
                }else if(vibrate){
                    channelName=CHANNEL_VIBRATE;
                    channel=iniChannel(channelName);
                    channel.enableVibration(true);
                    channel.setSound(null,null);
                }else {
                    channelName=CHANNEL_SILENT;
                    channel=iniChannel(channelName);
                    channel.setVibrationPattern(new long[]{0});
                    channel.setSound(null,null);
                }
                mNotificationManager.createNotificationChannel(channel);
            }
            Intent notificationIntent = new Intent(BackgroundRefresher.this, MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(BackgroundRefresher.this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            NotificationCompat.Builder b=new NotificationCompat.Builder(this,channelName);
            b.setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("空氣汙染警報")
                    .setContentText(city+"空氣品質"+dataProvider.Status+" AQI "+threshold)
                    .setDefaults(Notification.DEFAULT_LIGHTS)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(contentIntent);
            //鈴聲設定(8.0 down)
            if(vibrate){
                b.setDefaults(Notification.DEFAULT_VIBRATE);
            }
            if(ring){
                b.setDefaults(Notification.DEFAULT_SOUND);
            }
            mNotificationManager.notify(0, b.build());
            Log.e(TAG,"Notify Sent");
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationChannel iniChannel(String channelName){
        return new NotificationChannel(channelName,
                "空氣汙染警報", NotificationManager.IMPORTANCE_DEFAULT);
    }
}
