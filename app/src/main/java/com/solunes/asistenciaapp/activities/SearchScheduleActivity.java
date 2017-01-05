package com.solunes.asistenciaapp.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.solunes.asistenciaapp.R;

public class SearchScheduleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_schedule);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }
}
