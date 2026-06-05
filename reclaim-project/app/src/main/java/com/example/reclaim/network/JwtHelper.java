package com.example.reclaim.network;

import android.util.Base64;

import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Lightweight JWT payload decoder for extracting the user ID (subject)
 * from a stored access token. Signature verification is performed by the server.
 */
public final class JwtHelper {

    private JwtHelper() {
    }

    @Nullable
    public static String getUserId(@Nullable String token) {
        JSONObject payload = decodePayload(token);
        return payload != null ? payload.optString("sub", null) : null;
    }

    /**
     * Returns {@code true} when the token is missing, malformed, or past its {@code exp} claim.
     */
    public static boolean isExpired(@Nullable String token) {
        JSONObject payload = decodePayload(token);
        if (payload == null) {
            return true;
        }

        long expSeconds = payload.optLong("exp", 0L);
        if (expSeconds <= 0L) {
            return true;
        }

        long nowSeconds = System.currentTimeMillis() / 1000L;
        return nowSeconds >= expSeconds;
    }

    @Nullable
    private static JSONObject decodePayload(@Nullable String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }

        try {
            byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            return new JSONObject(new String(decoded));
        } catch (Exception e) {
            return null;
        }
    }
}
