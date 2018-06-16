package com.yuntech.eb211.airpollutiondetector;

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
    String SiteName,Status="",PublishTime;
    float PM25=0,NO2=0,O3=0;
    //建構子
    DataProvider(LocationProvider locationProvider){
        CurrentLocationProvider=locationProvider;
    }
    public void getNearestStation(){
        String location=null;
        location=CurrentLocationProvider.getLocation();
        if(location==null){
            location=CurrentLocationProvider.getLocation();
        }
        AQdata(location);

    }
    private void AQdata(String location){
        final ExecutorService service = Executors.newSingleThreadExecutor();
        final String county=location;
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
                        float dis=Float.MAX_VALUE;
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject jsonStation = array.getJSONObject(i);
                            float Latitude = Float.valueOf(jsonStation.getString("Latitude"));
                            float Longitude = Float.valueOf(jsonStation.getString("Longitude"));
                            if(dis>CurrentLocationProvider.DistanceBetween(Latitude,Longitude)){
                                SiteName = jsonStation.getString("SiteName");
                                AQI=Integer.valueOf(jsonStation.getString("AQI"));
                                Status = jsonStation.getString("Status");
                                PM25=Float.valueOf(jsonStation.getString("PM2.5"));
                                O3=Float.valueOf(jsonStation.getString("O3"));
                                NO2=Float.valueOf(jsonStation.getString("NO2"));
                                PublishTime = jsonStation.getString("PublishTime");
                            }
                        }
                    }
                    catch(JSONException e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
