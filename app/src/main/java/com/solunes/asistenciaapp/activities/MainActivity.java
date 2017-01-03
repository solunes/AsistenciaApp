package com.solunes.asistenciaapp.activities;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

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
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.solunes.asistenciaapp.ItemSchedule;
import com.solunes.asistenciaapp.R;
import com.solunes.asistenciaapp.Schedule;
import com.solunes.asistenciaapp.adapters.ScheduleRecyclerViewAdapter;
import com.solunes.asistenciaapp.networking.CallbackAPI;
import com.solunes.asistenciaapp.networking.GetRequest;
import com.solunes.asistenciaapp.networking.Token;
import com.solunes.asistenciaapp.services.LocationService;
import com.solunes.asistenciaapp.utils.UserPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements LocationService.LocationServiceCallBack {
    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private ArrayList<ItemSchedule> itemSchedules;
    private LocationService locationService;
    private Intent intentService;
    private String actualSchedule;
    private String token;

    private Button buttonAction;
    private View progressButton;

    private static final String KEY_INSIDE = "inside";
    private boolean inside;
    private boolean selectMethod;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        recyclerView = (RecyclerView) findViewById(R.id.list);
        itemSchedules = new ArrayList<>();
        checkProviderEnabled();

        requestData();

        recyclerView.setAdapter(new ScheduleRecyclerViewAdapter(this, itemSchedules));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        token = UserPreferences.getString(this, Token.KEY_TOKEN);
        inside = UserPreferences.getBoolean(this, KEY_INSIDE);
        progressButton = findViewById(R.id.progress_button);
        buttonAction = (Button) findViewById(R.id.button_action);
        if (inside) {
            buttonAction.setText(R.string.button_text_out);
        } else {
            buttonAction.setText(R.string.button_text_in);
        }
        buttonAction.setVisibility(View.VISIBLE);
        buttonAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inside) {
                    validateButton(getResources().getString(R.string.button_text_out));
                } else {
                    validateButton(getResources().getString(R.string.button_text_in));
                }
            }
        });
        startService();
        timer = new Timer();
    }

    private void startService() {
        intentService = new Intent(this, LocationService.class);
        if (!isServiceRunning()) {
            startService(intentService); //Starting the service
            bindService(intentService, connection, Context.BIND_AUTO_CREATE); //Binding to the service!
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_schedule:
                startActivity(new Intent(MainActivity.this, SearchScheduleActivity.class));
                return true;
            case R.id.action_notifications:
                startActivity(new Intent(MainActivity.this, NotificationActivity.class));
                return true;
            case R.id.action_method:
                item.setChecked(!item.isChecked());
                locationService.stopRequest(selectMethod);
                selectMethod = item.isChecked();
                Log.e(TAG, "onOptionsItemSelected: " + selectMethod);
                return true;
            case R.id.action_logout:
                UserPreferences.putBoolean(this, LoginActivity.KEY_LOGIN, false);

                finish();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestData() {
        new GetRequest(token, "http://asistencia.solunes.com/api/check-location/1/check/-16.489689/68.119294/0.0/" + methodLocation(), new CallbackAPI() {
            @Override
            public void onSuccess(String result, int statusCode) {
                try {
                    JSONObject jsonObjectRoot = new JSONObject(result);
                    JSONObject jsonObjectActualSchedule = jsonObjectRoot.getJSONObject("actual_schedule");
                    String in = jsonObjectActualSchedule.getString("in");
                    String out = jsonObjectActualSchedule.getString("out");
                    actualSchedule = in + " - " + out;
                    requestSchedule(jsonObjectRoot);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                recyclerView.setAdapter(new ScheduleRecyclerViewAdapter(getApplicationContext(), itemSchedules));
            }

            @Override
            public void onFailed(String reason, int statusCode) {
                Log.e(TAG, "onFailed: " + reason);
            }
        }).execute();
    }

    private void requestSchedule(JSONObject jsonObjectRoot) throws JSONException {
        JSONObject jsonObject = jsonObjectRoot.getJSONObject("schedules");
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String next = keys.next();
            ItemSchedule itemSchedule = new ItemSchedule();
            itemSchedule.setDate(next);
            ArrayList<Schedule> schedules = new ArrayList<>();
            JSONArray jsonArray = jsonObject.getJSONArray(next);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject objectSchedule = jsonArray.getJSONObject(i);
                Schedule schedule = new Schedule();
                schedule.setIn(objectSchedule.getString("in"));
                schedule.setOut(objectSchedule.getString("out"));
                if (!objectSchedule.isNull("observations")) {
                    JSONArray observations = objectSchedule.getJSONArray("observations");
                    ArrayList<String> strings = new ArrayList<>();
                    for (int j = 0; j < observations.length(); j++) {
                        strings.add(observations.getString(j));
                    }
                    schedule.setObservations(strings);
                } else {
                    schedule.setObservations(new ArrayList<String>());
                }
                schedules.add(schedule);
            }
            itemSchedule.setSchedules(schedules);
            itemSchedules.add(itemSchedule);
        }
    }

    private void checkProviderEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.e(TAG, "checkProviderEnabled: show dialog");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 7);
            }
            return;
        }
        boolean providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!providerEnabled) {
            launchSettingsGeolocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 7: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "onRequestPermissionsResult: tenemos GPS");
                } else {
                    Log.e(TAG, "onRequestPermissionsResult: fuck!!!");
                }
            }
        }
    }

    public void launchSettingsGeolocation() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.dialog_title_geolocation)
                .setMessage(R.string.dialog_message_geolocation)
                .setPositiveButton(R.string.dialog_action_accept, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent gpsOptionsIntent = new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(gpsOptionsIntent);
                    }

                })
                .setNegativeButton(R.string.dialog_action_cancel, null)
                .show();
    }

    public void validateButton(String text) {
        new AlertDialog.Builder(this)
                .setTitle("Validación")
                .setMessage("¿Desea continuar con la acción de " + text + "?")
                .setPositiveButton(R.string.dialog_action_accept, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (inside) {
                            Location locationOut = locationService.getLocationOut(selectMethod);
                            sendLocationOut(locationOut);
                        } else {
                            if (locationService.getCurrentLocationIn(selectMethod)) {
                                Snackbar.make(recyclerView, "obteniendo ubicación", Snackbar.LENGTH_SHORT).show();
                                buttonAction.setVisibility(View.INVISIBLE);
                                progressButton.setVisibility(View.VISIBLE);
                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        locationService.removeListener();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                buttonAction.setVisibility(View.VISIBLE);
                                                progressButton.setVisibility(View.GONE);
                                            }
                                        });
                                    }
                                    // TODO: 29-12-16 tiempo para el timer
                                }, 1000 * 60 * 5);
                            } else {
                                Snackbar.make(recyclerView, "vuelva a intentar", Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    }

                })
                .setNegativeButton(R.string.dialog_action_cancel, null)
                .show();
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.e(TAG, "onService Connected");
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            locationService = binder.getServiceInstance(); //Get instance of your service!
            locationService.registerClient(MainActivity.this); //Activity register in the service as client for callabcks!
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.e(TAG, "onService Disconnected");
        }
    };

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.solunes.asistenciaapp.services.LocationService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(intentService, connection, Context.BIND_AUTO_CREATE);
        cancelNotification();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        showNotfication();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void getCurrentLocation(int actionId, Location currentLocation) {
        // TODO: 29-12-16 validar para action in
        switch (actionId) {
            case LocationService.ACTION_IN:
                sendLocationIn(currentLocation);
                break;
            case LocationService.ACTION_CHECK:
                sendLocationCheck(currentLocation);
                break;
            case LocationService.ACTION_OUT:
                break;
        }
        Log.e(TAG, "getCurrentLocation: " +
                currentLocation.getLatitude() + " | " +
                currentLocation.getLongitude() + " | " +
                currentLocation.getAccuracy());
    }

    private void sendLocationIn(Location currentLocation) {
        String url = "http://asistencia.solunes.com/api/check-location/1/in/" + currentLocation.getLatitude() + "/" + currentLocation.getLongitude() + "/" + currentLocation.getAccuracy() + "/" + methodLocation();
        new GetRequest(token, url, new CallbackAPI() {
            @Override
            public void onSuccess(String result, int statusCode) {
                JSONObject jsonObjectRoot = null;
                try {
                    jsonObjectRoot = new JSONObject(result);
                    Log.e(TAG, "onSuccess: " + result);
                    boolean inLocation = jsonObjectRoot.getBoolean("in_location");
                    int distance = jsonObjectRoot.getInt("distance");
                    Log.e(TAG, "onSuccess: " + inLocation + " | " + distance);
                    if (inLocation) {
                        timer.cancel();
                        UserPreferences.putBoolean(getApplicationContext(), KEY_INSIDE, true);
                        inside = true;
                        buttonAction.setText(R.string.button_text_out);
                        buttonAction.setVisibility(View.VISIBLE);
                        progressButton.setVisibility(View.INVISIBLE);
                        locationService.removeListener();
                        locationService.getCurrentLocationCheck(selectMethod);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailed(String reason, int statusCode) {

            }
        }).execute();
    }

    private void sendLocationCheck(Location currentLocation) {
        String url = "http://asistencia.solunes.com/api/check-location/1/check/" + currentLocation.getLatitude() + "/" + currentLocation.getLongitude() + "/" + currentLocation.getAccuracy() + "/" + methodLocation();
        new GetRequest(token, url, new CallbackAPI() {
            @Override
            public void onSuccess(String result, int statusCode) {
                JSONObject jsonObjectRoot = null;
                try {
                    jsonObjectRoot = new JSONObject(result);

                    boolean inLocation = jsonObjectRoot.getBoolean("in_location");
                    int distance = jsonObjectRoot.getInt("distance");
                    Log.e(TAG, "onSuccess: " + inLocation + " | " + distance);

                    // TODO: 29-12-16 guardar datos en la base de datos para pruebas
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailed(String reason, int statusCode) {

            }
        }).execute();
    }

    private void sendLocationOut(Location locationOut) {
        locationService.removeListener();
        String url = "http://asistencia.solunes.com/api/check-location/1/out/" + locationOut.getLatitude() + "/" + locationOut.getLongitude() + "/" + locationOut.getAccuracy() + "/" + methodLocation();
        new GetRequest(token, url, new CallbackAPI() {
            @Override
            public void onSuccess(String result, int statusCode) {
                JSONObject jsonObjectRoot = null;
                try {
                    jsonObjectRoot = new JSONObject(result);
                    boolean inLocation = jsonObjectRoot.getBoolean("in_location");
                    int distance = jsonObjectRoot.getInt("distance");
                    Log.e(TAG, "onSuccess: " + inLocation + " | " + distance);
                    buttonAction.setText(R.string.button_text_in);
                    UserPreferences.putBoolean(getApplicationContext(), KEY_INSIDE, false);
                    inside = false;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailed(String reason, int statusCode) {

            }
        }).execute();
    }

    private String methodLocation() {
        if (selectMethod) {
            return "google";
        } else {
            return "gps";
        }
    }

    private void showNotfication() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_schedule_white_24dp)
                .setTicker("Asistencia App")
                .setAutoCancel(false)
                .setContentTitle("Horario Actual")
                .setContentText(actualSchedule)
                .build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Notification.FLAG_ONGOING_EVENT, notification);
    }

    private void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Notification.FLAG_ONGOING_EVENT);
    }
}
