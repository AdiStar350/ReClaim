package com.example.reclaim.notifications;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.reclaim.network.FcmTokenRequest;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fetches the device FCM token and registers it with the backend so the
 * server can address pushes to this user. Safe to call on every app start;
 * skips silently when the user is not logged in.
 */
public final class FcmTokenRegistrar {

    private static final String TAG = "FcmTokenRegistrar";

    private FcmTokenRegistrar() {
    }

    public static void register(@NonNull Context context) {
        Context app = context.getApplicationContext();
        if (TokenManager.getAuthHeader(app) == null) {
            return;
        }
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> sendToken(app, token))
                .addOnFailureListener(e -> Log.w(TAG, "Could not fetch FCM token", e));
    }

    public static void sendToken(@NonNull Context context, @NonNull String token) {
        String authHeader = TokenManager.getAuthHeader(context);
        if (authHeader == null) {
            Log.d(TAG, "Not logged in; FCM token will be registered after login");
            return;
        }
        RetrofitClient.getApiService()
                .updateFcmToken(authHeader, new FcmTokenRequest(token))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "FCM token registered with backend");
                        } else {
                            Log.w(TAG, "FCM token registration failed http=" + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Log.w(TAG, "FCM token registration failed", t);
                    }
                });
    }
}
