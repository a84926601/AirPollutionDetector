package com.yuntech.eb211.airpollutiondetector;

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
    Context BackgroundRefresherContext;
    private static final String TAG  = "LocationProvider";
    private LocationManager mLocationManager = null;
    Location mLastLocation;
    LocationProvider(Context context){
        BackgroundRefresherContext=context;
        initializeLocationManager();
    }
    private class LocationListener implements android.location.LocationListener{
        public LocationListener(String provider)
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
    LocationListener[] mLocationListeners = new LocationListener[] {
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
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }
    public String getLocation() {
        for (int i = mLocationListeners.length - 1; i >= 0; i--) {   //順序決定精準度
            try {
                mLocationManager.requestSingleUpdate(
                        LocationManager.NETWORK_PROVIDER, mLocationListeners[i], null);
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "network provider does not exist, " + ex.getMessage());
            }
        }
        String Address="No data";
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
        Log.e(TAG, "the falg is " + flag);
        try {
            //根据经纬度获取地理位置信息
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String addressText = String.format("%s-%s%s%s%s",
                        address.getCountryName(), //國家
                        address.getAdminArea(), //城市
                        address.getLocality(), //區
                        address.getThoroughfare(), //路
                        address.getSubThoroughfare() //巷號
                );
                return addressText;
            }
        }catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(TAG,"位置解析錯誤");
            e.printStackTrace();
        }
        return null;
    }
}
