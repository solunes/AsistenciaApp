package com.solunes.asistenciaapp.services;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.solunes.asistenciaapp.R;
import com.solunes.asistenciaapp.activities.MainActivity;

public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private static final String TAG = "LocationService";
    public static final int ACTION_IN = 0;
    public static final int ACTION_CHECK = 1;
    public static final int ACTION_OUT = 2;
    private static final int MIN_DISTANCE = 1;
    private static final int MIN_TIME = (1000 * 5);

    private LocationServiceCallBack serviceCallBack;
    private final IBinder mBinder = new LocalBinder();

    private LocationManager locationManager;
    private AsistenciaLocationListener locationListener;
    private Criteria criteria;

    @Override
    public void onCreate() {
        super.onCreate();
        if (locationManager == null) {
            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }

        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);

        googleLocationServices();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e(TAG, "onLocationChanged: " + location.getLatitude() + " | " + location.getLongitude() + " | " + location.getAccuracy());
    }


    public class LocalBinder extends Binder {
        public LocationService getServiceInstance() {
            return LocationService.this;
        }
    }

    public void registerClient(Activity activity) {
        this.serviceCallBack = (LocationServiceCallBack) activity;
    }

    private boolean getCurrentLocation(int minTime) {
        String provider = locationManager.getBestProvider(criteria, true);
        if (provider != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "onCreate: no provided");
                return false;
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(provider, minTime, 1, locationListener);
                return true;
            } else {
                return false;
            }
        } else {
            Log.e(TAG, "onCreate: provider null");
            return false;
        }
    }

    public boolean getCurrentLocationIn(boolean method) {
        if (method) {
            stopLocationUpdates();
            return startLocationUpdates();
        } else {
            removeListener();
            locationListener = new AsistenciaLocationListener(ACTION_IN);
            return getCurrentLocation(5000);
        }
    }

    public boolean getCurrentLocationCheck(boolean method) {
        if (method) {
            stopLocationUpdates();
            return startLocationUpdates();
        } else {
            removeListener();
            locationListener = new AsistenciaLocationListener(ACTION_CHECK);
            return getCurrentLocation(10000);
        }
    }

    public Location getLocationOut(boolean method) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "getLocationOut: fuck");
            return null;
        }
        if (method) {
            return LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        } else {
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
    }

    public void stopRequest(boolean method){
        if (method){
            stopLocationUpdates();
        } else {
            removeListener();
        }
    }

    private GoogleApiClient googleApiClient;

    private void googleLocationServices() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        googleApiClient.connect();

        createLocationRequest();
    }

    private LocationRequest mLocationRequest;

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient,
                        builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                final LocationSettingsStates locationSettingsStates = locationSettingsResult.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
//                        try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        Log.e(TAG, "onResult: RESOLUTION_REQUIRED");
//                            status.startResolutionForResult(
//                                    OuterClass.this,
//                                    REQUEST_CHECK_SETTINGS);
//                        } catch (IntentSender.SendIntentException e) {
//                            // Ignore the error.
//                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.e(TAG, "onResult: SETTINGS_CHANGE_UNAVAILABLE");
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    private boolean startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "startLocationUpdates: no hay permisos");
            return false;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, mLocationRequest, this);
        return true;
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    public void onDestroy() {
        googleApiClient.disconnect();
        super.onDestroy();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Notification.FLAG_ONGOING_EVENT);
    }

    public void removeListener() {
        if (locationManager != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "onDestroy: not provided");
                    return;
                }
                locationManager.removeUpdates(locationListener);
            } catch (Exception ex) {
                Log.i(TAG, "fail to remove location listners, ignore", ex);
            }
        }
    }

    private class AsistenciaLocationListener implements LocationListener {
        private int actionId;

        public AsistenciaLocationListener(int actionId) {
            this.actionId = actionId;
        }

        @Override
        public void onLocationChanged(Location location) {
            serviceCallBack.getCurrentLocation(actionId, location);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            Log.e(TAG, "onStatusChanged: " + s + " | " + i + " | " + bundle.toString());
        }

        @Override
        public void onProviderEnabled(String s) {
            Log.e(TAG, "onProviderEnabled: " + s);
        }

        @Override
        public void onProviderDisabled(String s) {
            Log.e(TAG, "onProviderDisabled: " + s);
        }
    }

    public interface LocationServiceCallBack {
        void getCurrentLocation(int actionId, Location currentLocation);
    }
}
