```java
private RetrofitClient() { }
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep Retrofit as a shared static setup.
Where it works: I am not called. Java uses me when someone tries `new RetrofitClient()`.
Why I chose to write the code of the function the way its written: I made the constructor private so getApiService is the only entry.
More: I use a private constructor. The class is final.

```java
public static synchronized Retrofit getInstance() {
    if (retrofit == null) {
        // Logs request and response bodies
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(RetrofitClient::interceptBackendAvailability)
                .addInterceptor(loggingInterceptor)
                // Tunnels and mobile can be slow
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
    return retrofit;
}
```

What that function does: I build one Retrofit client the first time, then reuse it.
What the function is responsible for: I hold the backend URL, timeouts, logging, and the availability interceptor.
Where it works: I am called by getApiService. Screens do not call me by name.
Why I chose to write the code of the function the way its written: I use synchronized and a null check so two threads do not build two clients. 30s timeouts fit a tunnel.
More: I use OkHttpClient, HttpLoggingInterceptor.Level.BODY, GsonConverterFactory, and interceptBackendAvailability.

```java
private static Response interceptBackendAvailability(Interceptor.Chain chain)
        throws IOException {
    Request request = chain.request().newBuilder()
            .header("ngrok-skip-browser-warning", "true")
            .build();
    try {
        Response response = chain.proceed(request);
        if (BackendAvailability.isUnreachableStatus(response.code())) {
            BackendAvailability.markUnreachable();
            OfflineProtocol.onBackendUnreachable();
        } else {
            BackendAvailability.markReachable();
            if (response.isSuccessful() && isDataMutation(request)) {
                AppStateEvents.notifyMutation();
            }
        }
        return response;
    } catch (IOException e) {
        if (!chain.call().isCanceled()) {
            BackendAvailability.markUnreachable();
            OfflineProtocol.onBackendUnreachable();
        }
        throw e;
    }
}
```

What that function does: I skip the ngrok warning page, mark if the API is up, and open the offline dialog on failure.
What the function is responsible for: I watch every HTTP call without each screen repeating that work.
Where it works: OkHttp runs me on each request from getInstance. Canceled calls do not count as down.
Why I chose to write the code of the function the way its written: I only notify AppStateEvents on successful writes, and I ignore cancel so leaving a screen does not look like an outage.
More: I use the ngrok-skip-browser-warning header, BackendAvailability, OfflineProtocol.onBackendUnreachable, and AppStateEvents.notifyMutation.

```java
private static boolean isDataMutation(Request request) {
    String method = request.method();
    if (!"POST".equals(method) && !"PUT".equals(method)
            && !"PATCH".equals(method) && !"DELETE".equals(method)) {
        return false;
    }
    String path = request.url().encodedPath();
    if (path.contains("/api/auth/") || path.contains("fcm-token")) {
        return false;
    }
    return true;
}
```

What that function does: I return true for writes that change item or claim data.
What the function is responsible for: I keep login and push-token calls from refreshing item lists.
Where it works: I am called by interceptBackendAvailability after a successful response.
Why I chose to write the code of the function the way its written: Auth and fcm-token paths are skipped because they do not change reports.
More: I use Request.method and encodedPath.

```java
public static synchronized ReClaimApiService getApiService() {
    if (apiService == null) {
        apiService = getInstance().create(ReClaimApiService.class);
    }
    return apiService;
}
```

What that function does: I return the shared API service, built on first use.
What the function is responsible for: I am how screens and workers talk to the backend.
Where it works: I am called from LoginActivity, RegisterActivity, VerifyOtpActivity, ResetPasswordActivity, HomeFragment, ItemsViewModel, ItemDetailsActivity, ReportActivity, MyItemsFragment, ReportHistoryActivity, PendingClaimsActivity, PendingClaimsSheet, ProfileSettingsFragment, EditProfileActivity, ReportSyncWorker, and FcmTokenRegistrar.
Why I chose to write the code of the function the way its written: I reuse one Retrofit proxy so interceptors stay on every call.
More: I use Retrofit.create with ReClaimApiService.
