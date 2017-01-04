package com.solunes.asistenciaapp.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.solunes.asistenciaapp.R;
import com.solunes.asistenciaapp.models.User;
import com.solunes.asistenciaapp.networking.Token;
import com.solunes.asistenciaapp.utils.StringUtils;
import com.solunes.asistenciaapp.utils.UserPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import static com.solunes.asistenciaapp.networking.Token.KEY_EXPIRATION_DATE;
import static com.solunes.asistenciaapp.networking.Token.KEY_TOKEN;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    public static final String KEY_LOGIN = "login";
    public static final String KEY_LOGIN_ID = "user_id";
    public static final String KEY_SCHEDULES = "schedules";

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
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
                    User user = new User();
                    user.setUsername(LoginActivity.this.user.getText().toString());
                    user.setPassword(pass.getText().toString());
                    Token.tokenRequest(user, new Token.CallbackToken() {
                        @Override
                        public void onToken(String token) {
                            Log.e(TAG, "onToken: " + token);
                            startActivity();
                        }

                        @Override
                        public void onSuccessToken(String result) {
                            Log.e(TAG, "onSuccessToken: " + result);
                            JSONObject jsonObject = null;
                            try {
                                jsonObject = new JSONObject(result);
                                String token = jsonObject.getString("token");
                                String expirationDate = jsonObject.getString("expirationDate");
                                int userId = jsonObject.getInt("user_id");
                                UserPreferences.putString(getApplicationContext(), KEY_TOKEN, token);
                                UserPreferences.putString(getApplicationContext(), KEY_EXPIRATION_DATE, expirationDate);
                                UserPreferences.putInt(getApplicationContext(), KEY_LOGIN_ID, userId);
                                UserPreferences.putString(getApplicationContext(), KEY_SCHEDULES, jsonObject.getString("schedules"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            startActivity();
                        }

                        @Override
                        public void onFailToken(String reason) {
                            Log.e(TAG, "onFailToken: " + reason);
                        }
                    });
                }
            }
        });
    }

    private void startActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        UserPreferences.putBoolean(LoginActivity.this, KEY_LOGIN, true);
        finish();
    }
}
