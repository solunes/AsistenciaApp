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

public class LocationService extends Service implements com.google.android.gms.location.LocationListener {
    private static final String TAG = "LocationService";
    public static final int ACTION_IN = 0;
    public static final int ACTION_CHECK = 1;
    public static final int ACTION_OUT = 2;

    private LocationServiceCallBack serviceCallBack;
    private final IBinder mBinder = new LocalBinder();
    private AsistenciaLocationListener locationListener;

    private boolean googleMethod;
    private boolean lastLocation;

    private NativeLocation nativeLocation;
    private GoogleLocation googleLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        nativeLocation = new NativeLocation(getApplicationContext());
        googleLocation = new GoogleLocation(getApplicationContext());
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
    public void onLocationChanged(Location location) {
        serviceCallBack.getCurrentLocation(actionId, location);
    }

    public class LocalBinder extends Binder {
        public LocationService getServiceInstance() {
            return LocationService.this;
        }
    }

    public void registerClient(Activity activity) {
        this.serviceCallBack = (LocationServiceCallBack) activity;
    }

    private int actionId;

    private Location getCurrentLocation() {
        Log.e(TAG, "getCurrentLocation: " + googleMethod);
        Location location = null;
        if (googleMethod) {
            location = googleLocation.getLocation(lastLocation, this);
        } else {
            nativeLocation.removeListener(locationListener);
            if (lastLocation) {
                location = nativeLocation.getLastLocation();
            } else {
                nativeLocation.requestLocation(getApplicationContext(), 1000, locationListener);
            }
        }
        return location;
    }

    public Location getCurrentLocationIn() {
        actionId = ACTION_IN;
        locationListener = new AsistenciaLocationListener(ACTION_IN);
        // 5000
        return getCurrentLocation();
    }

    public Location getCurrentLocationCheck() {
        actionId = ACTION_CHECK;
        locationListener = new AsistenciaLocationListener(ACTION_CHECK);
        // 1000 * 60
        Log.e(TAG, "getCurrentLocationCheck: ");
        return getCurrentLocation();
    }

    public Location getLocationOut() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "getLocationOut: fuck");
            return null;
        }
        if (googleMethod) {
            googleLocation.stopLocationUpdates(this);
            return googleLocation.lastLocation();
        } else {
            nativeLocation.removeListener(locationListener);
            return nativeLocation.getLastLocation();
        }
    }

    public void stopRequest() {
        Log.e(TAG, "stopRequest: ");
        if (googleMethod) {
            googleLocation.stopLocationUpdates(this);
        } else {
            nativeLocation.removeListener(locationListener);
        }
    }

    @Override
    public void onDestroy() {
        if (googleMethod) {
            googleLocation.disconnect();
        }
        super.onDestroy();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Notification.FLAG_ONGOING_EVENT);
    }

    public class AsistenciaLocationListener implements LocationListener {
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

    public void setGoogleMethod(boolean googleMethod) {
        this.googleMethod = googleMethod;
    }

    public void setLastLocation(boolean lastLocation) {
        this.lastLocation = lastLocation;
    }

    public boolean isLastLocation() {
        return this.lastLocation;
    }
}
