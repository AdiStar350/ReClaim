# RetrofitClient.java

## Overview
This file is a **singleton helper** that builds and shares the app's networking
setup. A singleton is a class you never create with `new`; instead everyone calls its
static methods and shares one hidden instance. `RetrofitClient` builds one `Retrofit`
object (the library that turns `ReClaimApiService` into real HTTP calls) and one API
service, then hands the same ones back every time. See Retrofit and OkHttp in
`docs/GLOSSARY.md`.

Building networking once and reusing it matters because the setup here (timeouts,
logging, and a custom interceptor that watches whether the backend is reachable)
should apply to every call in the app.

## Concepts used here
- **Retrofit.** The library that creates the API from `ReClaimApiService`; see
  `docs/GLOSSARY.md`.
- **OkHttp + Interceptor.** OkHttp is the lower-level HTTP client Retrofit uses. An
  interceptor is a hook that runs on every request/response; here it adds a header and
  detects outages. See `docs/GLOSSARY.md`.
- **Gson converter.** `GsonConverterFactory` tells Retrofit to convert JSON to/from
  Java objects with Gson; see `docs/GLOSSARY.md`.
- **Singleton with lazy init.** The `Retrofit` and API objects are built the first
  time they are asked for, then cached in static fields.
- **`synchronized`.** A Java keyword that stops two threads from running the same
  method at once, so two threads cannot build two clients.
- **Method reference (`::`).** `RetrofitClient::interceptBackendAvailability` passes a
  method as the interceptor, instead of writing an anonymous class.

## Libraries and imports
- **Retrofit (`retrofit2.Retrofit`, `retrofit2.converter.gson.GsonConverterFactory`).**
  `Retrofit.Builder` sets the base URL, the HTTP client, and the Gson converter;
  `retrofit.create(...)` builds the API proxy.
- **OkHttp (`okhttp3.OkHttpClient`, `Interceptor`, `Request`, `Response`,
  `okhttp3.logging.HttpLoggingInterceptor`).** `OkHttpClient.Builder` adds
  interceptors and sets timeouts. `HttpLoggingInterceptor` logs full request/response
  bodies for debugging. `Interceptor.Chain`, `Request`, and `Response` are used inside
  the custom interceptor.
- **Project classes.** `AppStateEvents` (to signal that data changed after a
  successful write), `BackendAvailability` (to record reachable/unreachable), and
  `OfflineProtocol` (to pop the offline dialog on failure).
- **Java standard.** `java.io.IOException` (thrown by network I/O) and
  `java.util.concurrent.TimeUnit` (to express timeouts in seconds).

## Fields
- `private static final String BASE_URL` — the backend address. Uses an ngrok tunnel
  URL so the app works off the local network; the trailing slash is required by
  Retrofit.
- `private static Retrofit retrofit` — the shared Retrofit instance, built once and
  cached.
- `private static ReClaimApiService apiService` — the shared API service proxy, built
  once and cached.

## Functions

### `private RetrofitClient()`
```java
private RetrofitClient() { }
```
- **What it does:** an empty private constructor that blocks anyone from writing
  `new RetrofitClient()`.
- **Where it's called:** nothing calls it. Java requires a constructor to exist;
  making it private keeps the class usable only through its static methods.
- **Why it's written this way:** this class is a pure static helper (a singleton), so
  instances would be pointless and possibly confusing.
- **Step by step:** 1) declared private; 2) body is empty; 3) because it is private,
  the compiler prevents outside code from creating an object.

### `private static Response interceptBackendAvailability(Interceptor.Chain chain)`
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
- **What it does:** runs on every HTTP call. It adds a header so ngrok does not show a
  warning page, records whether the backend answered, opens the offline dialog on
  failure, and signals a data refresh after a successful write.
- **Where it's called:** OkHttp runs it automatically for each request, because
  `getInstance` registers it with `.addInterceptor(RetrofitClient::interceptBackendAvailability)`.
  No app code calls it directly.
- **Why it's written this way:** putting this logic in one interceptor means every
  screen benefits without repeating outage checks. Canceled calls are ignored so
  leaving a screen mid-request does not look like a server outage. Only successful
  *writes* trigger a refresh, so read calls do not cause refresh loops.
- **Step by step:**
  1. Copy the outgoing request and add the `ngrok-skip-browser-warning: true` header.
  2. Send it with `chain.proceed(request)`.
  3. If the status code means the server is down (see `BackendAvailability`), mark it
     unreachable and tell `OfflineProtocol` to show the dialog.
  4. Otherwise mark it reachable; if the call succeeded and it was a data mutation,
     call `AppStateEvents.notifyMutation()` so screens reload.
  5. Return the response.
  6. If sending throws an `IOException`, and the call was not canceled, mark the
     backend unreachable and show the dialog; then rethrow so the caller still sees
     the error.

### `private static boolean isDataMutation(Request request)`
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
- **What it does:** returns `true` only for HTTP calls that actually change item or
  claim data on the server.
- **Where it's called:** by `interceptBackendAvailability`, after a successful
  response, to decide whether to trigger a refresh.
- **Why it's written this way:** login/OTP calls (`/api/auth/`) and push-token saves
  (`fcm-token`) are writes too, but they do not change the lists shown on screen, so
  they are excluded to avoid needless reloads.
- **Step by step:**
  1. Read the HTTP method; if it is not `POST`/`PUT`/`PATCH`/`DELETE`, it is a read,
     so return `false`.
  2. Read the URL path.
  3. If the path is an auth path or an fcm-token path, return `false`.
  4. Otherwise it is a real data change, so return `true`.

### `public static synchronized Retrofit getInstance()`
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
- **What it does:** builds the shared `Retrofit` object the first time it is needed,
  then returns that same object on later calls.
- **Where it's called:** by `getApiService` in this class. Screens do not call it by
  name.
- **Why it's written this way:** `synchronized` plus the null check makes sure two
  threads cannot both build a client (a safe lazy singleton). The 30-second timeouts
  suit a slow tunnel and mobile networks. Two interceptors are attached: the outage
  watcher and the body logger.
- **Step by step:**
  1. If `retrofit` is already built, skip to the return.
  2. Create a logging interceptor set to log full bodies.
  3. Build an `OkHttpClient` with the outage interceptor, the logger, and 30s
     connect/read/write timeouts.
  4. Build the `Retrofit` with the base URL, that client, and the Gson converter.
  5. Store it in the static field and return it.

### `public static synchronized ReClaimApiService getApiService()`
```java
public static synchronized ReClaimApiService getApiService() {
    if (apiService == null) {
        apiService = getInstance().create(ReClaimApiService.class);
    }
    return apiService;
}
```
- **What it does:** returns the shared API service (the object that implements
  `ReClaimApiService`), building it on first use.
- **Where it's called:** widely across the app, including `LoginActivity`,
  `RegisterActivity`, `VerifyOtpActivity`, `ResetPasswordActivity`, `HomeFragment`,
  `ItemsViewModel`, `ItemDetailsActivity`, `ReportActivity`, `MyItemsFragment`,
  `ReportHistoryActivity`, `PendingClaimsActivity`, `PendingClaimsSheet`,
  `ProfileSettingsFragment`, `EditProfileActivity`, `ReportSyncWorker`, and
  `FcmTokenRegistrar`.
- **Why it's written this way:** reusing one API proxy means the interceptors and
  settings from `getInstance` apply to every call. `synchronized` keeps the lazy build
  thread-safe.
- **Step by step:**
  1. If `apiService` is already built, return it.
  2. Otherwise call `getInstance()` and ask Retrofit to `create` the
     `ReClaimApiService` proxy.
  3. Cache it and return it.
