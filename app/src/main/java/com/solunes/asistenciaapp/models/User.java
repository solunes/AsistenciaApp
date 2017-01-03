package com.solunes.asistenciaapp.models;

import com.solunes.asistenciaapp.networking.AbstractUser;

/**
 * Created by jhonlimaster on 03-01-17.
 */

public class User extends AbstractUser {

    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
