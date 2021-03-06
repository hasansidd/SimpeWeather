package com.siddapps.android.simpleweather.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.text.DateFormat;
import java.util.List;

public class LocationUtil {
    private static final String TAG = "LocationUtil";
    private LocationManager mLocationManager;
    private Context mContext;

    public LocationUtil(Context context) {
        mContext = context;
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    public Location getCurrentLocationLatLon() throws Exception {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Location permissions not granted");
            return null;
        }
        return getLastKnownLocation();
    }

    private Location getLastKnownLocation() {

        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Location permissions not granted");
        }

        for (String provider : providers) {
            Location location = mLocationManager.getLastKnownLocation(provider);
            //Log.i(TAG, String.format("Location provider : %s, accuracy: %s", provider, location.getAccuracy()));

            if (location == null) {
                continue;
            }

            if (bestLocation == null || location.getTime() > bestLocation.getTime()) {
                if (bestLocation == null) {
                    Log.i(TAG, String.format("Location data from %s : %s", location.getProvider(), android.text.format.DateFormat.format("MM-dd hh:mm:ss a", location.getTime())));
                } else {
                    Log.i(TAG, String.format("Location data from %s : %s > Location data from %s : %s", location.getProvider(), android.text.format.DateFormat.format("MM-dd hh:mm:ss a", location.getTime()), bestLocation.getProvider(), android.text.format.DateFormat.format("MM-dd hh:mm:ss a", bestLocation.getTime())));
                }
                bestLocation = location;
            }

        }

        if (bestLocation == null) {
            Log.i(TAG, "Last known location not available from any provider");
        }
        return bestLocation;
    }
}
