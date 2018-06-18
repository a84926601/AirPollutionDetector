package com.yuntech.eb211.airpollutiondetector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public class LocationProvider {
    private Context BackgroundRefresherContext;
    private static final String TAG  = "LocationProvider";
    private LocationManager mLocationManager = null;
    private Location mLastLocation;
    String AdminArea,Locality;
    LocationProvider(Context context){
        BackgroundRefresherContext=context;
        initializeLocationManager();
    }
    private class LocationListener implements android.location.LocationListener{
        LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }
        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
        }
        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }
        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }
    private LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };
    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) BackgroundRefresherContext.getSystemService(Context.LOCATION_SERVICE);
        }
    }
    public void destroyLocationManager(){
        if (mLocationManager != null) {
            for (LocationListener mLocationListener : mLocationListeners) {
                try {
                    mLocationManager.removeUpdates(mLocationListener);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }
    @SuppressLint("MissingPermission")
    String getLocation() {
        Location bestResult = null;
        float bestAccuracy = Float.MAX_VALUE;
        long bestTime = Long.MIN_VALUE;
        List<String> matchingProviders = mLocationManager.getAllProviders();
        for (String provider: matchingProviders) {
            Location location = mLocationManager.getLastKnownLocation(provider);
            if (location != null) {
                float accuracy = location.getAccuracy();
                long time = location.getTime();

                if (accuracy < bestAccuracy) {
                    bestResult = location;
                    bestAccuracy = accuracy;
                    bestTime = time;
                }
                else if (bestAccuracy == Float.MAX_VALUE && time > bestTime) {
                    bestResult = location;
                    bestTime = time;
                }
            }
        }
        mLastLocation=bestResult;
        try {
            mLocationManager.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER, mLocationListeners[0], null);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestSingleUpdate(
                    LocationManager.NETWORK_PROVIDER, mLocationListeners[1], null);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        String Address=null;
        if(mLastLocation.getTime()!=0){
            try{
                Address=getAddress(mLastLocation);
            }catch (IOException e){
                Log.e(TAG,"未初始化Location");
            }
        }
        return Address;
    }

    private String getAddress(Location location) throws IOException {
        Geocoder geocoder = new Geocoder(BackgroundRefresherContext);
        boolean flag = geocoder.isPresent();
        Log.e(TAG, "the flag is " + flag);
        try {
            //根据经纬度获取地理位置信息
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                Log.e(TAG,address.toString());
                Locality=address.getLocality()!=null?address.getLocality():address.getSubLocality();
                AdminArea=address.getAdminArea()!=null?address.getAdminArea():address.getSubAdminArea();
                if(AdminArea.contentEquals("臺灣省"))AdminArea=address.getSubAdminArea();    //省轄市
                AdminArea=AdminArea.replace("台","臺"); //因轉譯出資料與線上資料不同
                Log.e(TAG,AdminArea);
                return AdminArea;
            }
        }catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(TAG,"位置解析錯誤");
            e.printStackTrace();
        }
        return null;
    }
    public float DistanceBetween(double Latitude,double Longitude){
        float results[]=new float[1];
        Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(),Latitude,Longitude,results);
        return results[0];
    }
}
