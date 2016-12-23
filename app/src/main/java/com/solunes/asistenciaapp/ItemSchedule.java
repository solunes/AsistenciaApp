package com.solunes.asistenciaapp;

import java.util.ArrayList;

/**
 * Created by jhonlimaster on 21-12-16.
 */

public class ItemSchedule {
    private String date;
    private ArrayList<Schedule> schedules;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public ArrayList<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(ArrayList<Schedule> schedules) {
        this.schedules = schedules;
    }

}
