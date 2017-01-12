package com.solunes.asistenciaapp.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * Created by jhonlimaster on 11-01-17.
 */

public class NativeLocation {
    private static final String TAG = "NativeLocation";
    private LocationManager locationManager;
    private Criteria criteria;
    private Context context;

    public NativeLocation(Context context) {
        this.context = context;
        if (locationManager == null) {
            locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
        }
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
    }

    public boolean requestLocation(Context context, int minTime, LocationService.AsistenciaLocationListener asistenciaLocationListener) {
        Log.e(TAG, "requestLocation");
        String provider = locationManager.getBestProvider(criteria, true);
        if (provider != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "onCreate: no provided");
                return false;
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(provider, minTime, 1, asistenciaLocationListener);
                return true;
            } else {
                return false;
            }
        } else {
            Log.e(TAG, "onCreate: provider null");
            return false;
        }
    }

    public Location getLastLocation() {
        Log.e(TAG, "isLastLocation");
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "isLastLocation: no permission");
            return null;
        }
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    public void removeListener(LocationService.AsistenciaLocationListener asistenciaLocationListener) {
        Log.e(TAG, "removeListener");
        if (locationManager != null) {
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "removeListener: no permission");
                    return;
                }
                locationManager.removeUpdates(asistenciaLocationListener);
            } catch (Exception ex) {
//                Log.i(TAG, "fail to remove location listners, ignore", ex);
            }
        }
    }
}
