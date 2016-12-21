package com.solunes.asistenciaapp.networking;

/**
 * Interface para las respuestas en los casos de exito y fracaso
 */
public interface CallbackAPI {
    void onSuccess(String result, int statusCode);
    void onFailed(String reason, int statusCode);
}