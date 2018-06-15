package com.yuntech.eb211.airpollutiondetector;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    private static final String TAG= "AirPollutionDetector";
    private static final int REQUEST_CODE_PERMISSIONS_LOCATION = 1;
    TextView mTextView;
    static final String database="https://opendata.epa.gov.tw/webapi/api/rest/datastore/355000000I-000259/?format=json&"
            ,token="coTaajMn6EqYsoCEMtXkFQ";
    String county="雲林縣",coor="";
    OkHttpClient client = new HttpsUtils().getTrustAllClient();
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
        mTextView = (TextView) findViewById(R.id.TextView1);
        location_requiresPermissions();
        //showAQI();
    }
    private void showAQI(){
        final ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(new Runnable() {
            @Override
            public void run() {
                Request request = new Request.Builder()
                        .url(database+"filters=County eq '"+county+"'&token="+token)
                        .build();
                try {
                    final Response response = client.newCall(request).execute();
                    final String resStr = response.body().string();
                    try{
                        //建立一個JSONObject並帶入JSON格式文字，getString(String key)取出欄位的數值
                        JSONArray array = new JSONObject(resStr).getJSONObject("result").getJSONArray("records");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject jsonStation = array.getJSONObject(i);
                            String SiteName = jsonStation.getString("SiteName");
                            String Latitude = jsonStation.getString("Latitude");
                            String Longitude = jsonStation.getString("Longitude");
                            coor+=("("+SiteName+","+Latitude+","+Longitude+") ");
                        }
                    }
                    catch(JSONException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextView.setText(coor);
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private void setupBackgroundService() {
        final Intent myIntent = new Intent(MainActivity.this, BackgroundRefresher.class);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startService(myIntent);
            }
        }, 8000);
    }
        @AfterPermissionGranted(REQUEST_CODE_PERMISSIONS_LOCATION)
        private void location_requiresPermissions() {
            String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            if (EasyPermissions.hasPermissions(this, perms)) {
                setupBackgroundService();
            } else {
                EasyPermissions.requestPermissions(this,
                        "未允許「" + getString(R.string.app_name) + "」取得裝置位置權限，將使「" + getString(R.string.app_name) + "」無法正常運作，是否重新設定權限？",
                        REQUEST_CODE_PERMISSIONS_LOCATION,
                        perms);
            }
        }
}