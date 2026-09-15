```java
private TokenManager() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep this class as static methods only.
Where it works: I am not called. Java uses me when someone tries `new TokenManager()`.
Why I chose to write the code of the function the way its written: I made the constructor private so the token lives only in shared preferences.
More: I use a private constructor. The class is final.

```java
public static void saveToken(@NonNull Context context, @NonNull String token) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    prefs.edit().putString(KEY_TOKEN, token).apply();
    AuthSessionListener.onAuthenticated(context);
}
```

What that function does: I save the login token and tell the app a session just started.
What the function is responsible for: I persist the token and start guest-report claim after login or sign-up.
Where it works: I am called by LoginActivity, VerifyOtpActivity, and ResetPasswordActivity after a successful token reply.
Why I chose to write the code of the function the way its written: I call AuthSessionListener after apply so queued reports can attach to this user.
More: I use SharedPreferences.apply and AuthSessionListener.onAuthenticated.

```java
@Nullable
public static String getToken(@NonNull Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    return prefs.getString(KEY_TOKEN, null);
}
```

What that function does: I return the saved token, or null if none is stored.
What the function is responsible for: I am the raw read used by the header, expiry check, and user id.
Where it works: I am called by getAuthHeader, hasToken, and getUserId in this class. Other classes do not call me by name.
Why I chose to write the code of the function the way its written: I keep one read path so the key name stays in one place.
More: I use SharedPreferences.getString with KEY_TOKEN.

```java
@Nullable
public static String getAuthHeader(@NonNull Context context) {
    String token = getToken(context);
    return token != null ? "Bearer " + token : null;
}
```

What that function does: I build the Authorization header from the saved token.
What the function is responsible for: I give Retrofit calls the Bearer value they need.
Where it works: I am called from HomeFragment, ItemsViewModel, ItemDetailsActivity, ReportActivity, MyItemsFragment, ReportHistoryActivity, PendingClaimsActivity, PendingClaimsSheet, ProfileSettingsFragment, EditProfileActivity, ReportSyncWorker, and FcmTokenRegistrar.
Why I chose to write the code of the function the way its written: I return null with no token so callers can stop instead of sending a bad header.
More: I use getToken and prefix the value with "Bearer ".

```java
public static void clearToken(@NonNull Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    prefs.edit().remove(KEY_TOKEN).apply();
}
```

What that function does: I delete the saved token.
What the function is responsible for: I sign the device out of stored auth.
Where it works: I am called by ProfileSettingsFragment on logout and delete account, and by hasToken when the token is empty or expired.
Why I chose to write the code of the function the way its written: I only remove KEY_TOKEN so other prefs stay.
More: I use SharedPreferences.edit().remove and apply.

```java
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
```

What that function does: I return true only if a stored token is still in date. I drop a bad one.
What the function is responsible for: I am the signed-in check for the first screen and login.
Where it works: I am called by MainActivity.onCreate and LoginActivity.onCreate.
Why I chose to write the code of the function the way its written: I clear expired tokens so a stale value cannot keep sending the user to the dashboard.
More: I use JwtHelper.isExpired and clearToken.

```java
@Nullable
public static String getUserId(@NonNull Context context) {
    return JwtHelper.getUserId(getToken(context));
}
```

What that function does: I return the user id from the saved token payload.
What the function is responsible for: I let screens and sync know who is signed in without a profile call.
Where it works: I am called from HomeFragment, ItemsViewModel, ItemDetailsActivity, ReportActivity, EditProfileActivity, PendingReportStore, and ReportSyncWorker.
Why I chose to write the code of the function the way its written: I reuse JwtHelper so I do not parse the token twice.
More: I use getToken and JwtHelper.getUserId.
