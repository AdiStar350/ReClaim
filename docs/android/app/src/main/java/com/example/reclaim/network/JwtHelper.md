```java
private JwtHelper() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep this class as static methods only.
Where it works: I am not called. Java uses me when someone tries `new JwtHelper()`.
Why I chose to write the code of the function the way its written: I made the constructor private so callers use getUserId and isExpired.
More: I use a private constructor. The class is final.

```java
@Nullable
public static String getUserId(@Nullable String token) {
    JSONObject payload = decodePayload(token);
    return payload != null ? payload.optString("sub", null) : null;
}
```

What that function does: I read the user id from the token payload `sub` field.
What the function is responsible for: I give the app a user id without calling the server.
Where it works: I am called by TokenManager.getUserId with the saved token.
Why I chose to write the code of the function the way its written: I return null if the payload is missing so callers can treat a bad token as no user.
More: I use decodePayload and JSONObject.optString. I do not check the signature here.

```java
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
```

What that function does: I treat a missing, broken, or past-`exp` token as expired.
What the function is responsible for: I tell TokenManager when a stored token should be dropped.
Where it works: I am called by TokenManager.hasToken.
Why I chose to write the code of the function the way its written: A missing payload or missing exp counts as expired so a bad token cannot keep the user signed in.
More: I use decodePayload, JSONObject.optLong, and System.currentTimeMillis in seconds.

```java
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
```

What that function does: I Base64-decode the middle part of the token into JSON.
What the function is responsible for: I am the shared parse step for getUserId and isExpired.
Where it works: I am called by getUserId and isExpired.
Why I chose to write the code of the function the way its written: I catch all decode errors and return null so a broken token does not crash the app.
More: I use android.util.Base64 URL_SAFE flags and org.json.JSONObject. The server still checks the signature.
