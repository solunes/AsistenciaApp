package com.solunes.asistenciaapp.activities;

import android.content.Intent;
import android.os.Build;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.solunes.asistenciaapp.R;
import com.solunes.asistenciaapp.networking.CallbackAPI;
import com.solunes.asistenciaapp.networking.GetRequest;
import com.solunes.asistenciaapp.utils.UserPreferences;

public class LoginActivity extends AppCompatActivity implements CallbackAPI {

    private static final String TAG = "LoginActivity";
    public static final String KEY_LOGIN = "login";
    public static final String KEY_LOGIN_ID = "user_id";

    private EditText user;
    private EditText pass;
    private TextInputLayout inputLayoutUser;
    private TextInputLayout inputLayoutPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (UserPreferences.getBoolean(this, KEY_LOGIN)) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimary));
        }

        inputLayoutUser = (TextInputLayout) findViewById(R.id.input_user);
        inputLayoutPass = (TextInputLayout) findViewById(R.id.input_pass);
        user = (EditText) findViewById(R.id.edit_user);
        pass = (EditText) findViewById(R.id.edit_pass);
        Button buttonSign = (Button) findViewById(R.id.btn_signup);
        buttonSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // validacion de campos
                boolean valid = true;
                if (user.getText().toString().isEmpty()) {
                    inputLayoutUser.setError("Campo requerido!!!");
                    valid = false;
                } else {
                    inputLayoutUser.setError(null);
                }
                if (pass.getText().toString().isEmpty()) {
                    inputLayoutPass.setError("Campo requerido!!!");
                    valid = false;
                } else {
                    inputLayoutPass.setError(null);
                }
                if (valid) {
                    // TODO: 21-12-16 consulta al API para obtener el usuario
//                    new GetRequest(null, "http://asistencia.solunes.com", LoginActivity.this).execute();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    UserPreferences.putBoolean(LoginActivity.this, KEY_LOGIN, true);
                    finish();
                }
            }
        });
    }

    @Override
    public void onSuccess(String result, int statusCode) {
        // TODO: 21-12-16 get user_id, token, expiration_date, schedules,
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        UserPreferences.putBoolean(LoginActivity.this, KEY_LOGIN, true);
        finish();
    }

    @Override
    public void onFailed(String reason, int statusCode) {
        Toast.makeText(LoginActivity.this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
    }
}
