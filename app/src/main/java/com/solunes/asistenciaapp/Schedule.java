package com.solunes.asistenciaapp;

import java.util.ArrayList;

/**
 * Created by jhonlimaster on 21-12-16.
 */

public class Schedule {
    private String in;
    private String out;
    private ArrayList<String> observations;
    private String statusIn;
    private String statusOut;

    public enum Status {
        pending, check, holding
    }

    public String getStatusIn() {
        return statusIn;
    }

    public void setStatusIn(String statusIn) {
        this.statusIn = statusIn;
    }

    public String getStatusOut() {
        return statusOut;
    }

    public void setStatusOut(String statusOut) {
        this.statusOut = statusOut;
    }

    public String getIn() {
        return in;
    }

    public void setIn(String in) {
        this.in = in;
    }

    public String getOut() {
        return out;
    }

    public void setOut(String out) {
        this.out = out;
    }

    public ArrayList<String> getObservations() {
        return observations;
    }

    public void setObservations(ArrayList<String> observations) {
        this.observations = observations;
    }
}
