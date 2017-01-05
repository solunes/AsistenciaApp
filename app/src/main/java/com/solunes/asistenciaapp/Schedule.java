package com.solunes.asistenciaapp;

import java.util.ArrayList;

/**
 * Created by jhonlimaster on 21-12-16.
 */

public class Schedule {
    private String in;
    private String out;
    private ArrayList<String> observations;
    private Status statusIn;
    private Status statusOut;

    public enum Status {
        pending, check, holding
    }

    public Status getStatusIn() {
        return statusIn;
    }

    public void setStatusIn(Status statusIn) {
        this.statusIn = statusIn;
    }

    public Status getStatusOut() {
        return statusOut;
    }

    public void setStatusOut(Status statusOut) {
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
