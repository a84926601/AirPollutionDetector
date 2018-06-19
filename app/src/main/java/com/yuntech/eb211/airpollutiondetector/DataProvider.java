package com.yuntech.eb211.airpollutiondetector;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DataProvider {
    //網路取得Data
    private LocationProvider CurrentLocationProvider;
    private static final String database="https://opendata.epa.gov.tw/webapi/api/rest/datastore/355000000I-000259/?format=json&"
            ,token="coTaajMn6EqYsoCEMtXkFQ";
    private OkHttpClient client = new HttpsUtils().getTrustAllClient();
    //Station結構
    int AQI=0;
    String SiteName,Status,PublishTime,TAG="Data";
    float PM25=0,NO2=0,O3=0;
    //建構子
    DataProvider(LocationProvider locationProvider){
        CurrentLocationProvider=locationProvider;
    }
    public void getNearestStation(MainActivity mainActivity,BackgroundRefresher backgroundRefresher){
        String location;
        location=CurrentLocationProvider.getLocation();
        if(location==null){
            location=CurrentLocationProvider.getLocation();
        }
        AQdata(mainActivity,backgroundRefresher,location);
    }
    private void AQdata(final MainActivity mainActivity,final BackgroundRefresher backgroundRefresher, String location){
        //Log.e(TAG,client.());
        final Handler handler=new Handler();
        final ExecutorService service = Executors.newSingleThreadExecutor();
        final String county=location;
        service.submit(new Runnable() {
            @Override
            public void run() {
                JSONArray array=null;
                String resStr = null;
                Request request = new Request.Builder()
                        .url(database+"filters=County eq '"+county+"'&token="+token)
                        .build();
                try {
                    Log.e(TAG,"submit request");
                    Log.e(TAG,request.toString());
                    final Response response = client.newCall(request).execute();
                    Log.e(TAG,response.toString());
                    resStr = response.body().string();
                } catch (IOException e) {
                    Log.e(TAG,"TimeOut");
                    e.printStackTrace();
                }
                try{
                    //建立一個JSONObject並帶入JSON格式文字，getString(String key)取出欄位的數值
                    array = new JSONObject(resStr).getJSONObject("result").getJSONArray("records");
                    //if(CurrentLocationProvider.Locality==null){CurrentLocationProvider.Locality=SiteName;}
                    Log.e(TAG,"Finish Download Data");
                }
                catch(JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG,"Download Failed");
                }
                float disMin=Float.MAX_VALUE;
                try {
                    if(array!=null){
                        for(int i = 0; i < array.length(); i++) {
                            JSONObject jsonStation = array.getJSONObject(i);
                            float Latitude = Float.valueOf(jsonStation.getString("Latitude"));
                            float Longitude = Float.valueOf(jsonStation.getString("Longitude"));
                            float CurrentDistence=CurrentLocationProvider.DistanceBetween(Latitude,Longitude);
                            if(disMin>CurrentDistence){
                                disMin=CurrentDistence;
                                SiteName = jsonStation.getString("SiteName");
                                String sAQI=jsonStation.getString("AQI"),sPM25=jsonStation.getString("PM2.5"),
                                        sO3=jsonStation.getString("O3"),sNO2=jsonStation.getString("NO2");
                                //數值如不存在

                                AQI=isNumber(sAQI)?Integer.valueOf(sAQI):0;
                                Status = jsonStation.getString("Status");
                                PM25=isNumber(sPM25)?Float.valueOf(sPM25):0;
                                O3=isNumber(sO3)?Float.valueOf(sO3):0;
                                NO2=isNumber(sNO2)?Float.valueOf(sNO2):0;
                                PublishTime = jsonStation.getString("PublishTime");
                            }
                        }
                    }
                }
                catch (JSONException e){
                    e.printStackTrace();
                    Log.e(TAG,"DataError");
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(AQI!=0){
                            if(mainActivity!=null) {
                                Log.e("Data", "Call ShowAQ");
                                int img=0;
                                String ColorString="",suggest="";
                                if(AQI<51){
                                    img=R.drawable.happy;
                                    ColorString=mainActivity.getString(R.string.AQI_excellent_color);
                                    suggest=mainActivity.getString(R.string.AQI_excellent_suggest);
                                }
                                else if(AQI<101){
                                    img=R.drawable.smiling;
                                    ColorString=mainActivity.getString(R.string.AQI_good_color);
                                    suggest=mainActivity.getString(R.string.AQI_good_suggest);
                                }
                                else if(AQI<151){
                                    img=R.drawable.sceptic;
                                    ColorString=mainActivity.getString(R.string.AQI_lightly_polluted_color);
                                    suggest=mainActivity.getString(R.string.AQI_lightly_polluted_suggest);
                                }
                                else if(AQI<201){
                                    img=R.drawable.sad;
                                    ColorString=mainActivity.getString(R.string.AQI_moderately_polluted_color);
                                    suggest=mainActivity.getString(R.string.AQI_moderately_polluted_suggest);
                                }
                                else if(AQI<301){
                                    img=R.drawable.face;
                                    ColorString=mainActivity.getString(R.string.AQI_heavily_polluted_color);
                                    suggest=mainActivity.getString(R.string.AQI_heavily_polluted_suggest);
                                }
                                else {
                                    img=R.drawable.dead;
                                    ColorString=mainActivity.getString(R.string.AQI_severely_polluted_color);
                                    suggest=mainActivity.getString(R.string.AQI_severely_polluted_suggest);
                                }

                                mainActivity.drawAQIcircle(AQI,ColorString);
                                mainActivity.showAQ(img,suggest);
                            }
                            else if(backgroundRefresher!=null) {
                                backgroundRefresher.sendAlertPushNotification(CurrentLocationProvider.AdminArea,AQI);
                                Log.e(TAG, "Call sendAlertPushNotification");
                            }
                        }
                        else Log.e(TAG,"AQI=0");
                    }
                });
            }
        });
    }
    static public boolean isNumber(String str){
        if(str.matches("\\d+(?:\\.\\d+)?"))
            return true;
        else
            return false;
    }
}
