package com.yuntech.eb211.airpollutiondetector;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;

import java.util.List;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,View.OnClickListener {
    private static final String TAG= "AirPollutionDetector";
    private static final int REQUEST_CODE_PERMISSIONS_LOCATION = 1,jobId=12;
    int series1Index;
    SeriesItem seriesItem1;
    LocationProvider locationProvider;
    DataProvider dataProvider;
    DecoView arcView;
    TextView locationview,cityview,timeview,AqiText,status,Pm25Text,O3Text;
    Button setting;
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
        getSupportActionBar().hide();
        arcView = findViewById(R.id.dynamicArcView);
        locationProvider = new LocationProvider(this);
        dataProvider = new DataProvider(locationProvider);
        findViewById(R.id.setting).setOnClickListener(this);
        locationview = findViewById(R.id.locationview);
        cityview = findViewById(R.id.cityview);
        timeview = findViewById(R.id.timeview);
        AqiText = findViewById(R.id.AqiText);
        status = findViewById(R.id.status);
        Pm25Text= findViewById(R.id.Pm25Text);
        O3Text = findViewById(R.id.O3Text);
        location_requiresPermissions();
        drawBaseCircle();
    }
    void drawData(int endPosition,String ColorString){
        Log.e(TAG,String.valueOf(endPosition));
        endPosition=numToPercent(endPosition);
        arcView.addEvent(new DecoEvent.Builder(endPosition)
                .setIndex(series1Index)
                .setDelay(500)
                .setDuration(2000)
                .setColor(Color.parseColor(ColorString))
                .build());
    }
    private void drawBaseCircle(){
        // Create background track
        arcView.addSeries(new SeriesItem.Builder(Color.argb(80, 0, 0, 0))
                .setRange(0, 100, 100)
                .setInitialVisibility(false)
                .setLineWidth(50f)
                .build());

        //Create data series track
        seriesItem1 = new SeriesItem.Builder(Color.argb(255, 0, 232, 0))
                .setRange(0, 100, 0)
                .setLineWidth(50f)
                .build();

        seriesItem1.addArcSeriesItemListener(new SeriesItem.SeriesItemListener() {
            @Override
            public void onSeriesItemAnimationProgress(float percentComplete, float currentPosition) {
                AqiText.setText(String.valueOf(percentToNum(currentPosition)));
            }

            @Override
            public void onSeriesItemDisplayProgress(float percentComplete) {

            }
        });
        series1Index = arcView.addSeries(seriesItem1);

        arcView.addEvent(new DecoEvent.Builder(DecoEvent.EventType.EVENT_SHOW, true)
                .setDelay(200)
                .setDuration(500)
                .build());
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.setting:
                Intent intent1 = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent1);
                break;
        }
    }
    private int percentToNum(float percentage){
        double num=(0.000627*Math.pow(percentage,3)-0.17*Math.pow(percentage,2)+15.73*percentage);
        return num<dataProvider.AQI?(int)num:dataProvider.AQI;
    }
    private int numToPercent(int num){
        return (int)(1.61484*Math.pow(10,-6)*Math.pow(num,3)-0.000675618*Math.pow(num,2)+0.134099*num);
    }
    private void setupBackgroundService() {
        Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
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
        },2000);

    }
    @AfterPermissionGranted(REQUEST_CODE_PERMISSIONS_LOCATION)
    private void location_requiresPermissions() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};
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
        //AqiText.setText(String.valueOf(dataProvider.AQI));
        status.setText(String.valueOf(dataProvider.Status));
        Pm25Text.setText(String.valueOf(dataProvider.PM25));
        O3Text.setText(String.valueOf(dataProvider.O3));
    }
}