```java
private FcmTokenRegistrar() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep token send as static methods.
Where it works: I am not called. Java uses me when someone tries `new FcmTokenRegistrar()`.
Why I chose to write the code of the function the way its written: I made the constructor private so ReclaimApp calls register.
More: I use a private constructor. The class is final.

```java
public static void register(@NonNull Context context) {
    Context app = context.getApplicationContext();
    if (TokenManager.getAuthHeader(app) == null) {
        return;
    }
    FirebaseMessaging.getInstance().getToken()
            .addOnSuccessListener(token -> sendToken(app, token))
            .addOnFailureListener(e -> Log.w(TAG, "Could not fetch FCM token", e));
}
```

What that function does: I fetch this device's FCM token and send it if the user is signed in.
What the function is responsible for: I am the start-up and dashboard token refresh.
Where it works: I am called by ReclaimApp.onCreate and DashboardActivity.
Why I chose to write the code of the function the way its written: I skip when there is no auth header so a logged-out start does not call the API.
More: I use FirebaseMessaging.getToken and sendToken. The success listener is a lambda.

```java
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
```

What that function does: I PUT the FCM token to the backend if a session exists.
What the function is responsible for: I am the HTTP send for a new or rotated token.
Where it works: I am called by register and by ReclaimMessagingService.onNewToken.
Why I chose to write the code of the function the way its written: I log failures and do not crash. The user can still use the app without push.
More: I use RetrofitClient.updateFcmToken and FcmTokenRequest.

```java
public void onResponse(@NonNull Call<Void> call,
                       @NonNull Response<Void> response) {
    if (response.isSuccessful()) {
        Log.d(TAG, "FCM token registered with backend");
    } else {
        Log.w(TAG, "FCM token registration failed http=" + response.code());
    }
}
```

What that function does: I log whether the token PUT succeeded.
What the function is responsible for: I am the Retrofit success path.
Where it works: Retrofit calls me from sendToken's enqueue.
Why I chose to write the code of the function the way its written: I only log. A failed code can retry on the next register.
More: I use Response.isSuccessful and Log.

```java
public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
    Log.w(TAG, "FCM token registration failed", t);
}
```

What that function does: I log a network failure while sending the token.
What the function is responsible for: I am the Retrofit failure path.
Where it works: Retrofit calls me from sendToken's enqueue.
Why I chose to write the code of the function the way its written: I do not show a toast. Push is optional.
More: I use Log.w with the throwable.
