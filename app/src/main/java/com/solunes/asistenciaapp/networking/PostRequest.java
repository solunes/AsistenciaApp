package com.solunes.asistenciaapp.networking;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Esta clase hace el manejo completo de las peticiones de tipo Post
 */
public class PostRequest extends AsyncTask<String, Void, String> {
    private Hashtable<String, String> params;
    private Hashtable<String, String> headers;
    private String urlEndpoint;
    private int statusCode;
    private String token;
    private static final String TAG = "PostRequest";
    private CallbackAPI callbackAPI;



    /**
     * Constructor de la clase
     * @param token token para la consulta, si no hay tiene que ser null
     * @param params un hashtable con los parametros para la consulta
     * @param headers un hashtable con header para la consulta
     * @param urlEndpoint un string con la url para la consulta
     * @param callbackAPI interface para hacer las respuestas
     */
    public PostRequest(String token, Hashtable<String, String> params, Hashtable<String, String> headers, String urlEndpoint, CallbackAPI callbackAPI) {
        this.params = params;
        this.urlEndpoint = urlEndpoint;
        this.headers = headers;
        this.callbackAPI = callbackAPI;
        this.token = token;
    }

    @Override
    protected String doInBackground(String... urls) {
        HttpURLConnection urlConnection = null;
        try {
            String paramString = getStringParams(params);
            Log.e(TAG, "Params: " + paramString);

            urlConnection = (HttpURLConnection) new URL(urlEndpoint).openConnection();
            urlConnection.setRequestMethod("POST");
            Log.e(TAG, "endpoint: " + urlEndpoint);

            if (this.token != null)
                urlConnection.setRequestProperty("Authorization", "Bearer " + this.token);

            if (headers != null && headers.size() > 0)
                for (String key : headers.keySet())
                    urlConnection.setRequestProperty(key, headers.get(key));

            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            out.write(paramString.getBytes());
            out.flush();
            out.close();

            urlConnection.connect();
            statusCode = urlConnection.getResponseCode();

            Log.e(TAG, "StatusCode: " + statusCode);

            if (isSuccessStatusCode()) {
                return getStringFromStream(urlConnection.getInputStream());
            } else {
                return getStringFromStream(urlConnection.getErrorStream());
            }

        } catch (IOException e) {
            Log.e(TAG, "Exception --------->>>>: " + e.getMessage());
            e.printStackTrace();

        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
        return null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void putHeader(String key, String value) {
        if (headers == null) {
            headers = new Hashtable<String, String>();
        }
        headers.put(key, value);
    }

    @Override
    protected void onPostExecute(String result) {

        if (getStatusCode() >= 200 && getStatusCode() <= 250) {
            callbackAPI.onSuccess(result, getStatusCode());
        } else {
            String messageError = "";
            try {
                JSONObject jsonObject = new JSONObject(result);
                messageError += jsonObject.getString("message");
                messageError += " (" + jsonObject.getString("status_code") + ")";
            } catch (JSONException e) {
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

    private static String getStringParams(Hashtable<String, String> params) {
        if (params.size() == 0)
            return "";

        StringBuffer buf = new StringBuffer();
        Enumeration<String> keys = params.keys();
        while (keys.hasMoreElements()) {
            buf.append(buf.length() == 0 ? "" : "&");
            String key = keys.nextElement();
            buf.append(key).append("=").append(params.get(key));
        }
        return buf.toString();

    }
}