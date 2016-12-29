package com.solunes.asistenciaapp.networking;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Hashtable;

/**
 * Created by jhonlimaster on 15-11-16.
 */

public class Token {

    private static final String TAG = "Token";
    public static final String KEY_TOKEN = "key_token";
    public static final String KEY_EXPIRATION_DATE = "key_expiration_date";

    /**
     * Este metodo valida si hay token y si no hay hace una consulta para obtener un nuevo token
     * @param user un usuario para obtener sus credenciales
     * @param callbackToken una inteface para hacer llegar la respuesta en la actividad de donde haya sido llamada
     */
    public static void getToken(String token, Date expirationDate,AbstractUser user, CallbackToken callbackToken) {
        if (token == null) {
            tokenRequest(user,callbackToken);
        } else {
            if (expirationDate.getTime() < System.currentTimeMillis()) {
                tokenRequest(user, callbackToken);
            } else {
                callbackToken.onToken(token);
            }
        }
    }

    /**
     * Este metodo hace la consulta del token al servidor
     * @param user usuario para mandar sus credenciales
     * @param callbackToken interface para responder en los casos de exito y fracaso
     */
    private static void tokenRequest(AbstractUser user, final CallbackToken callbackToken) {
        Hashtable<String, String> params = new Hashtable<>();
        params.put("username", user.getUsername());
        params.put("password", user.getPassword());
        new PostRequest("token", params, null, "http://asistencia.solunes.com/api-auth/authenticate", new CallbackAPI() {
            @Override
            public void onSuccess(String result, int statusCode) {
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(result);
                    Log.e(TAG, "onSuccess: token " + result);
                    String token = jsonObject.getString("token");
                    String expirationDate = jsonObject.getString("expirationDate");
//                    UserPreferences.putString(context, KEY_TOKEN, token);
//                    UserPreferences.putString(context, KEY_EXPIRATION_DATE, expirationDate);
                    callbackToken.onSuccessToken(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailed(String reason, int statusCode) {
                callbackToken.onFailToken(reason);
            }
        }).execute();
    }

    /**
     * interface para el token
     */
    public interface CallbackToken{
        void onToken(String token);
        void onSuccessToken(String result);
        void onFailToken(String reason);
    }
}
