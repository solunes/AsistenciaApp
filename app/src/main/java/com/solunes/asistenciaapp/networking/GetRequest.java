package com.solunes.asistenciaapp.networking;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;

/**
 * Esta clase hace el manejo de las peticiones de tipo GET
 */
public class GetRequest extends AsyncTask<String, Void, String> {
    private String TAG = GetRequest.class.getSimpleName();
    private Hashtable<String, String> headers;
    private String urlEndpoint;
    private int statusCode;
    private String token;
    private CallbackAPI callbackAPI;

    /**
     * Contructor para la clase
     *
     * @param token       token para la consulta, si no hay tiene que ser null
     * @param urlEndpoint url del endpoint para hacer la consulta
     * @param callbackAPI interface para las respuestas
     */
    public GetRequest(String token, String urlEndpoint, CallbackAPI callbackAPI) {
        this.headers = new Hashtable<>();
        this.urlEndpoint = urlEndpoint;
        this.callbackAPI = callbackAPI;
        this.token = token;
    }

    @Override
    protected String doInBackground(String... urls) {
        HttpURLConnection urlConnection = null;
        Log.e(TAG, "urlGET: " + urlEndpoint);

        try {
            urlConnection = (HttpURLConnection) new URL(urlEndpoint).openConnection();
            int TIMEOUT_VALUE = 10000;
            urlConnection.setReadTimeout(TIMEOUT_VALUE);
            urlConnection.setConnectTimeout(TIMEOUT_VALUE);
            urlConnection.setRequestMethod("GET");

            if (this.token != null) {
                urlConnection.setRequestProperty("Authorization", "Bearer " + this.token);
            }

            urlConnection.setDoInput(true);
            urlConnection.connect();
            statusCode = urlConnection.getResponseCode();

            if (isSuccessStatusCode()) {
                return getStringFromStream(urlConnection.getInputStream());
            } else {
                return getStringFromStream(urlConnection.getErrorStream());
            }

        } catch (IOException e) {
            Log.e(TAG, "Exception " + urlEndpoint + " --------->>>>: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void putHeader(String key, String value) {
        headers.put(key, value);
    }

    @Override
    protected void onPostExecute(String result) {
        if (isSuccessStatusCode()) {
            callbackAPI.onSuccess(result, getStatusCode());
        } else {
            String messageError = "";
            try {
                JSONObject jsonObject = new JSONObject(result);
                messageError += jsonObject.getString("message");
                messageError += " (" + jsonObject.getString("status_code") + ")";
            } catch (Exception e) {
                callbackAPI.onFailed(result, getStatusCode());
            }
            callbackAPI.onFailed(messageError, getStatusCode());
        }

    }

    public String getStringFromStream(InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder stringBuilder = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return stringBuilder.toString();
    }

    public boolean isSuccessStatusCode() {
        return (getStatusCode() >= 200 && getStatusCode() <= 250);
    }
}
