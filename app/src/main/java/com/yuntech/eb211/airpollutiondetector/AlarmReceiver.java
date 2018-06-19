package com.yuntech.eb211.airpollutiondetector;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    JobScheduler mJobScheduler;
    private static final int jobId=15;
    private static final String TAG="AlarmErceiver";
    private SharedPreferences Setting;
    @Override
    public void onReceive(final Context context, Intent intent)
    {
        Bundle bData = intent.getExtras();
        if(bData.get("msg").equals("alarm_push"))
        {
            Setting = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            mJobScheduler=(JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            Handler handler=new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    JobInfo.Builder builder = new JobInfo.Builder(jobId,
                            new ComponentName(context.getPackageName(), BackgroundRefresher.class.getName()));
                    builder.setPersisted(true)
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                            .setBackoffCriteria(0,JobInfo.BACKOFF_POLICY_LINEAR);
                    mJobScheduler.schedule(builder.build());
                    Log.e(TAG,"NowHasBeenScheduled");
                    MainActivity.setAlarm(Setting,context);
                }
            });
        }
    }
    //TODO 開機時復原排程
}
