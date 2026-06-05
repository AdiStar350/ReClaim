package com.example.reclaim.network;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper class for managing the JWT authentication token using
 * {@link SharedPreferences}.
 * <p>
 * The token is stored in a private preferences file
 * ({@code "reclaim_prefs"}) under the key {@code "jwt_token"}.
 * </p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * // Save token after login
 * TokenManager.saveToken(context, "eyJhbGciOi...");
 *
 * // Retrieve token for API calls
 * String token = TokenManager.getToken(context);
 *
 * // Build the Authorization header
 * String header = TokenManager.getAuthHeader(context);
 *   // → "Bearer eyJhbGciOi..."
 *
 * // Clear token on logout
 * TokenManager.clearToken(context);
 * }</pre>
 */
public final class TokenManager {

    /** Name of the SharedPreferences file. */
    private static final String PREFS_NAME = "reclaim_prefs";

    /** Key used to store the JWT token. */
    private static final String KEY_TOKEN = "jwt_token";

    // Prevent instantiation
    private TokenManager() {
    }

    /**
     * Persists the JWT token to SharedPreferences.
     *
     * @param context the application or activity context
     * @param token   the JWT token string to save
     */
    public static void saveToken(@NonNull Context context, @NonNull String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    /**
     * Retrieves the stored JWT token.
     *
     * @param context the application or activity context
     * @return the token string, or {@code null} if not set
     */
    @Nullable
    public static String getToken(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    /**
     * Returns the full {@code Authorization} header value for API calls.
     * <p>
     * Format: {@code "Bearer <token>"}
     * </p>
     *
     * @param context the application or activity context
     * @return the header string, or {@code null} if no token is stored
     */
    @Nullable
    public static String getAuthHeader(@NonNull Context context) {
        String token = getToken(context);
        return token != null ? "Bearer " + token : null;
    }

    /**
     * Removes the stored JWT token (e.g. on logout).
     *
     * @param context the application or activity context
     */
    public static void clearToken(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    /**
     * Checks whether a non-expired JWT token is currently stored.
     * Clears invalid or expired tokens so the user is routed back to login.
     *
     * @param context the application or activity context
     * @return {@code true} if a valid, unexpired token exists
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
     * Extracts the authenticated user ID from the stored JWT subject claim.
     *
     * @param context the application or activity context
     * @return user ID, or {@code null} if unavailable
     */
    @Nullable
    public static String getUserId(@NonNull Context context) {
        return JwtHelper.getUserId(getToken(context));
    }
}
