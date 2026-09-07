package com.example.reclaim.network;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.reclaim.sync.AuthSessionListener;

/**
 * Keeps the login token in shared preferences.
 */
public final class TokenManager {

    /** Preferences file name. */
    private static final String PREFS_NAME = "reclaim_prefs";

    /** Key for the token. */
    private static final String KEY_TOKEN = "jwt_token";

    // Not meant to be created
    private TokenManager() {
    }

    /**
     * Saves the token and tells {@link AuthSessionListener}.
     *
     * @param context app or activity context
     * @param token   the token to save
     */
    public static void saveToken(@NonNull Context context, @NonNull String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
        AuthSessionListener.onAuthenticated(context);
    }

    /**
     * The saved token, if any.
     *
     * @param context app or activity context
     * @return the token, or {@code null}
     */
    @Nullable
    public static String getToken(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    /**
     * Header value: {@code "Bearer <token>"}.
     *
     * @param context app or activity context
     * @return the header, or {@code null}
     */
    @Nullable
    public static String getAuthHeader(@NonNull Context context) {
        String token = getToken(context);
        return token != null ? "Bearer " + token : null;
    }

    /**
     * Drops the saved token.
     *
     * @param context app or activity context
     */
    public static void clearToken(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    /**
     * True if a token is stored and still valid.
     * Drops it if it is empty or past its time.
     *
     * @param context app or activity context
     * @return {@code true} if a good token exists
     */
    public static boolean hasToken(@NonNull Context context) {
        String token = getToken(context);
        if (token == null || token.isEmpty() || JwtHelper.isExpired(token)) {
            if (token != null) {
                clearToken(context);
            }
            return false;
        }
        return true;
    }

    /**
     * User id from the saved token.
     *
     * @param context app or activity context
     * @return user id, or {@code null}
     */
    @Nullable
    public static String getUserId(@NonNull Context context) {
        return JwtHelper.getUserId(getToken(context));
    }
}
