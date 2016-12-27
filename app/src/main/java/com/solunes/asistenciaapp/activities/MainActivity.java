package com.solunes.asistenciaapp.activities;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.solunes.asistenciaapp.ItemSchedule;
import com.solunes.asistenciaapp.R;
import com.solunes.asistenciaapp.Schedule;
import com.solunes.asistenciaapp.adapters.ScheduleRecyclerViewAdapter;
import com.solunes.asistenciaapp.networking.CallbackAPI;
import com.solunes.asistenciaapp.networking.GetRequest;
import com.solunes.asistenciaapp.services.LocationService;
import com.solunes.asistenciaapp.utils.UserPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements LocationService.LocationServiceCallBack {
    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private ArrayList<ItemSchedule> itemSchedules;
    private LocationService locationService;
    private Intent intentService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        recyclerView = (RecyclerView) findViewById(R.id.list);
        itemSchedules = new ArrayList<>();

        requestData();

        recyclerView.setAdapter(new ScheduleRecyclerViewAdapter(this, itemSchedules));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        intentService = new Intent(this, LocationService.class);
        Log.e(TAG, "onCreate: intent");
        if (!isServiceRunning()) {
            startService(intentService); //Starting the service
            Log.e(TAG, "onCreate: start service");
            bindService(intentService, connection, Context.BIND_AUTO_CREATE); //Binding to the service!
            Log.e(TAG, "onCreate:bind service ");
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
            case R.id.action_logout:
                UserPreferences.putBoolean(this, LoginActivity.KEY_LOGIN, false);
                finish();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestData() {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9";
        new GetRequest(token, "http://asistencia.solunes.com/api/check-location/1/check/-16.489689/68.119294", new CallbackAPI() {
            @Override
            public void onSuccess(String result, int statusCode) {
                try {
                    JSONObject jsonObjectRoot = new JSONObject(result);
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
                            Log.e(TAG, "onSuccess: " + objectSchedule);
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
                            Log.e(TAG, "onSuccess In: " + schedule.getIn());
                            schedules.add(schedule);
                        }
                        itemSchedule.setSchedules(schedules);
                        itemSchedules.add(itemSchedule);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < itemSchedules.size(); i++) {
                    ItemSchedule itemSchedule = itemSchedules.get(i);
                    Log.e(TAG, "itemSchedule: " + itemSchedule.getDate());
                    ArrayList<Schedule> schedules = itemSchedule.getSchedules();
                    for (int j = 0; j < schedules.size(); j++) {
                        Log.e(TAG, "schedules: " + schedules.get(j).getIn());
                    }
                }
                recyclerView.setAdapter(new ScheduleRecyclerViewAdapter(getApplicationContext(), itemSchedules));
            }

            @Override
            public void onFailed(String reason, int statusCode) {
                Log.e(TAG, "onFailed: " + reason);
            }
        }).execute();
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.e(TAG, "onService Connected");
            // We've binded to LocalService, cast the IBinder and get LocalService instance
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            locationService = binder.getServiceInstance(); //Get instance of your service!
            locationService.registerClient(MainActivity.this); //Activity register in the service as client for callabcks!
            locationService.getLocation();
//            tvServiceState.setText("Connected to service...");
//            tbStartTask.setEnabled(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.e(TAG, "onService Disconnected");
//            tvServiceState.setText("Service disconnected");
//            tbStartTask.setEnabled(false);
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
    }

    @Override
    public void getCurrentLocation(Location currentLocation) {
        // TODO: 26-12-16
        Log.e(TAG, "getCurrentLocation: " + currentLocation.getLatitude() + " - " + currentLocation.getLongitude());
    }
}
