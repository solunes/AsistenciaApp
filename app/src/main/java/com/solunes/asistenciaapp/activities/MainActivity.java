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
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
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
import android.widget.Toast;

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
    private static final String KEY_INSIDE = "inside";

    private RecyclerView recyclerView;
    private ArrayList<ItemSchedule> itemSchedules;
    private LocationService locationService;
    private Intent intentService;
    private String actualSchedule;
    private String token;
    private int userId;

    private Button buttonAction;
    private View progressButton;

    private boolean inside;
    private boolean selectMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        checkProviderEnabled();

        userId = UserPreferences.getInt(this, LoginActivity.KEY_LOGIN_ID);
        token = UserPreferences.getString(this, Token.KEY_TOKEN);
        inside = UserPreferences.getBoolean(this, KEY_INSIDE);
        progressButton = findViewById(R.id.progress_button);
        buttonAction = (Button) findViewById(R.id.button_action);
        try {
            requestSchedule(UserPreferences.getString(this, "schedules"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

    private Timer timer;

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
                selectMethod = item.isChecked();
                locationService.setGoogleMethod(selectMethod);
                locationService.stopRequest();
                return true;
            case R.id.action_last_location:
                item.setChecked(!item.isChecked());
                locationService.setLastLocation(item.isChecked());
                locationService.stopRequest();
                return true;
            case R.id.action_logout:
                UserPreferences.putBoolean(this, LoginActivity.KEY_LOGIN, false);
                finish();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestSchedule(String scheduleString) throws JSONException {
        JSONObject jsonObject = new JSONObject(scheduleString);
        Iterator<String> keys = jsonObject.keys();
        itemSchedules = new ArrayList<>();
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
                schedule.setStatusIn(getStatus(objectSchedule.getString("in_status")));
                schedule.setStatusOut(getStatus(objectSchedule.getString("out_status")));
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
        if (itemSchedules.size() > 0) {
            UserPreferences.putString(MainActivity.this, LoginActivity.KEY_SCHEDULES, scheduleString);
            recyclerView.setAdapter(new ScheduleRecyclerViewAdapter(getApplicationContext(), itemSchedules));
        }
    }

    private String getStatus(String status) {
        switch (status) {
            case "check":
                return Schedule.Status.check.name();
            case "holding":
                return Schedule.Status.holding.name();
            case "pending":
                return Schedule.Status.pending.name();
            default:
                return null;
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
                            timer.purge();
                            Location locationOut = locationService.getLocationOut();
                            sendLocationOut(locationOut);
                        } else {
                            Location location = locationService.getCurrentLocationIn();
                            Log.e(TAG, "onClick: " + location);
                            if (location == null) {
                                Snackbar.make(recyclerView, "obteniendo ubicación", Snackbar.LENGTH_SHORT).show();
                                if (locationService.isLastLocation()) {
                                    Toast.makeText(getApplicationContext(), "No hay ubicacion", Toast.LENGTH_SHORT).show();
                                } else {
                                    buttonAction.setVisibility(View.INVISIBLE);
                                    progressButton.setVisibility(View.VISIBLE);
                                }
                            } else {
                                sendLocationIn(location);
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
        Log.e(TAG, "getCurrentLocation: action ID " + actionId);
        Log.e(TAG, "getCurrentLocation: " +
                currentLocation.getLatitude() + " | " +
                currentLocation.getLongitude() + " | " +
                currentLocation.getAccuracy());
    }

    private void sendLocationIn(Location currentLocation) {
        String url = "http://asistencia.solunes.com/api/check-location/" + userId + "/in/" + currentLocation.getLatitude() + "/" + currentLocation.getLongitude() + "/" + currentLocation.getAccuracy() + "/" + methodLocation();
        new GetRequest(token, url, new CallbackAPI() {
            @Override
            public void onSuccess(String result, int statusCode) {
                JSONObject jsonObjectRoot = null;
                try {
                    jsonObjectRoot = new JSONObject(result);
                    Log.e(TAG, "onSuccess: " + result);
                    boolean inLocation = jsonObjectRoot.getBoolean("in_location");
                    int distance = jsonObjectRoot.getInt("distance");
                    actualSchedule = getActualSchedule(jsonObjectRoot.getString("actual_schedule"));
                    Log.e(TAG, "onSuccess: " + inLocation + " | " + distance);
                    requestSchedule(jsonObjectRoot.getString("schedules"));
                    if (inLocation) {
                        UserPreferences.putBoolean(getApplicationContext(), KEY_INSIDE, true);
                        inside = true;
                        buttonAction.setText(R.string.button_text_out);
                        Location locationCheck = locationService.getCurrentLocationCheck();
                        if (locationCheck != null) {
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    Location location = locationService.getCurrentLocationCheck();
                                    sendLocationCheck(location);
                                }
                            }, 1000 * 60 * 5);
                        }
                    }
                    buttonAction.setVisibility(View.VISIBLE);
                    progressButton.setVisibility(View.INVISIBLE);
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
        String url = "http://asistencia.solunes.com/api/check-location/" + userId + "/check/" + currentLocation.getLatitude() + "/" + currentLocation.getLongitude() + "/" + currentLocation.getAccuracy() + "/" + methodLocation();
        new GetRequest(token, url, new CallbackAPI() {
            @Override
            public void onSuccess(String result, int statusCode) {
                JSONObject jsonObjectRoot;
                try {
                    jsonObjectRoot = new JSONObject(result);

                    boolean inLocation = jsonObjectRoot.getBoolean("in_location");
                    int distance = jsonObjectRoot.getInt("distance");
                    actualSchedule = getActualSchedule(jsonObjectRoot.getString("actual_schedule"));
                    Log.e(TAG, "onSuccess: " + inLocation + " | " + distance);
                    // TODO: 09-01-17 validar que haya schedule
                    requestSchedule(jsonObjectRoot.getString("schedules"));
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
        locationService.stopRequest();
        String url = "http://asistencia.solunes.com/api/check-location/" + userId + "/out/" + locationOut.getLatitude() + "/" + locationOut.getLongitude() + "/" + locationOut.getAccuracy() + "/" + methodLocation();
        new GetRequest(token, url, new CallbackAPI() {
            @Override
            public void onSuccess(String result, int statusCode) {
                JSONObject jsonObjectRoot = null;
                try {
                    jsonObjectRoot = new JSONObject(result);
                    boolean inLocation = jsonObjectRoot.getBoolean("in_location");
                    int distance = jsonObjectRoot.getInt("distance");
                    actualSchedule = getActualSchedule(jsonObjectRoot.getString("actual_schedule"));
                    Log.e(TAG, "onSuccess: " + inLocation + " | " + distance);
                    requestSchedule(jsonObjectRoot.getString("schedules"));
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

    private String getActualSchedule(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        return jsonObject.getString("in") + " - " + jsonObject.getString("out");
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
        if (inside) {
            notificationManager.notify(Notification.FLAG_ONGOING_EVENT, notification);
        }
    }

    private void cancelNotification() {
        if (inside) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(Notification.FLAG_ONGOING_EVENT);
        }
    }
}
