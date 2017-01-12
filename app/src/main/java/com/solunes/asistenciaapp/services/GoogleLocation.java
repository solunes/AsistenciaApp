package com.solunes.asistenciaapp.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

/**
 * Created by jhonlimaster on 11-01-17.
 */

public class GoogleLocation implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GoogleLocation";
    private GoogleApiClient googleApiClient;
    private LocationRequest mLocationRequest;

    public GoogleLocation(Context context) {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        googleApiClient.connect();

        createLocationRequest();
    }

    public Location getLocation(boolean lastLocation, LocationListener locationListener) {
        Log.e(TAG, "getLocation: last " + lastLocation);
        if (lastLocation) {
            return lastLocation();
        } else {
            Log.e(TAG, "getLocation: listener " + locationListener);
            requestLocationUpdates(locationListener);
            return null;
        }
    }

    public void requestLocationUpdates(LocationListener locationListener) {
        if (ActivityCompat.checkSelfPermission(googleApiClient.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(googleApiClient.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "lastLocation: no permission");
            return;
        }
        Log.e(TAG, "requestLocationUpdates: " + googleApiClient.isConnected());
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, locationListener);
    }

    public Location lastLocation() {
        if (ActivityCompat.checkSelfPermission(googleApiClient.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(googleApiClient.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "lastLocation: no permission");
            return null;
        }
        return LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    }

    public void stopLocationUpdates(LocationListener locationListener) {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationListener);
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        // 1000 * 30
        mLocationRequest.setInterval(1000);
        // 1000 * 60
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
//                final LocationSettingsStates locationSettingsStates = locationSettingsResult.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.e(TAG, "onResult: RESOLUTION_REQUIRED");
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.e(TAG, "onResult: SETTINGS_CHANGE_UNAVAILABLE");
                        break;
                }
            }
        });
    }

    public void disconnect() {
        googleApiClient.disconnect();
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
}
