package com.yuntech.eb211.airpollutiondetector;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import java.util.List;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    private static final String TAG= "AirPollutionDetector";
    private static final int REQUEST_CODE_PERMISSIONS_LOCATION = 1,jobId=12;
    LocationProvider locationProvider;
    DataProvider dataProvider;
    TextView locationview,cityview,timeview,AqiText,status,Pm25Text,O3Text;
    //授權處理
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        setupBackgroundService();
    }
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, list)){
            new AppSettingsDialog.Builder(this).build().show();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE){
            Log.d(TAG, "onActivityResult: ");
        }
    }
    //開始運行
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationProvider = new LocationProvider(this);
        dataProvider = new DataProvider(locationProvider);
        locationview = findViewById(R.id.locationview);
        cityview = findViewById(R.id.cityview);
        timeview = findViewById(R.id.timeview);
        AqiText = findViewById(R.id.AqiText);
        status = findViewById(R.id.status);
        Pm25Text= findViewById(R.id.Pm25Text);
        O3Text = findViewById(R.id.O3Text);
        location_requiresPermissions();
    }

    private void setupBackgroundService() {
        JobScheduler mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        mJobScheduler.cancelAll();
        boolean hasBeenScheduled = false ;
        for ( JobInfo jobInfo : mJobScheduler.getAllPendingJobs() ) {
            if ( jobInfo.getId() == jobId ) {
                hasBeenScheduled = true ;
                Log.e(TAG,jobInfo.toString()+" wasScheduled");
                break ;
            }
        }
        if(!hasBeenScheduled){
            JobInfo.Builder builder = new JobInfo.Builder(jobId,
                    new ComponentName(getPackageName(), BackgroundRefresher.class.getName()));
            builder.setPersisted(true)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(0,JobInfo.BACKOFF_POLICY_LINEAR);

            mJobScheduler.schedule(builder.build());
            Log.e(TAG,"NowHasBeenScheduled");
        }
    }
    @AfterPermissionGranted(REQUEST_CODE_PERMISSIONS_LOCATION)
    private void location_requiresPermissions() {
        String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION};  //Manifest.permission.ACCESS_FINE_LOCATION
        if (EasyPermissions.hasPermissions(this, perms)) {
            dataProvider.getNearestStation(this,null);
            setupBackgroundService();
        } else {
            EasyPermissions.requestPermissions(this,
                    "未允許「" + getString(R.string.app_name) + "」取得裝置位置權限，將使「" + getString(R.string.app_name) + "」無法正常運作，是否重新設定權限？",
                    REQUEST_CODE_PERMISSIONS_LOCATION,
                    perms);
        }
    }
    public void showAQ(){
        locationview.setText(locationProvider.AdminArea);
        cityview.setText(locationProvider.Locality);
        timeview.setText(dataProvider.PublishTime);
        AqiText.setText(String.valueOf(dataProvider.AQI));
        status.setText(String.valueOf(dataProvider.Status));
        Pm25Text.setText(String.valueOf(dataProvider.PM25));
        O3Text.setText(String.valueOf(dataProvider.O3));
    }
}