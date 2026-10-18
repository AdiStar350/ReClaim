package com.example.reclaim.network;

import com.example.reclaim.data.AppStateEvents;
import com.example.reclaim.sync.BackendAvailability;
import com.example.reclaim.sync.OfflineProtocol;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Shared Retrofit setup and the API service.
 */
public final class RetrofitClient {

    /**
     * Backend URL. Use the ngrok domain so it works off the local network.
     * Local testing can use a LAN IP or {@code http://10.0.2.2:8080/}.
     * Trailing slash is required.
     */
    private static final String BASE_URL = "https://decal-magnetism-coherent.ngrok-free.dev/";

    /**
     * Used only when the ngrok tunnel is not running ({@code tunnel.sh} stopped).
     * {@code 10.0.2.2} is the host machine as seen from the Android emulator;
     * a physical device would need the host's LAN IP here instead.
     */
    private static final HttpUrl LOCAL_FALLBACK_URL = HttpUrl.get("http://10.0.2.2:8080/");

    /** How long to stay on the local fallback before trying the tunnel again. */
    private static final long FALLBACK_RETRY_MS = TimeUnit.SECONDS.toMillis(30);

    /** Until when requests go straight to the local fallback. 0 = use the tunnel. */
    private static volatile long useLocalUntil = 0L;

    /** Shared Retrofit instance. */
    private static Retrofit retrofit;

    /** Shared API service. */
    private static ReClaimApiService apiService;

    // Not meant to be created
    private RetrofitClient() { }

    /**
     * Shared {@link Retrofit}, built on first use.
     *
     * @return the Retrofit instance
     */
    public static synchronized Retrofit getInstance() {
        if (retrofit == null) {
            // Logs request and response bodies
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(RetrofitClient::interceptBackendAvailability)
                    .addInterceptor(RetrofitClient::interceptLocalFallback)
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

    /**
     * Sends requests to the ngrok tunnel first. If the tunnel is offline
     * (connection error, or an ngrok error page such as ERR_NGROK_3200),
     * retries the same request against the local backend and keeps using it
     * for a short while before checking the tunnel again.
     */
    private static Response interceptLocalFallback(Interceptor.Chain chain)
            throws IOException {
        Request request = chain.request();
        IOException tunnelError = null;
        boolean triedTunnel = false;

        if (System.currentTimeMillis() >= useLocalUntil) {
            triedTunnel = true;
            try {
                Response response = chain.proceed(request);
                if (!isNgrokTunnelError(response)) {
                    return response;
                }
                response.close();
            } catch (IOException e) {
                if (chain.call().isCanceled()) {
                    throw e;
                }
                tunnelError = e;
            }
            useLocalUntil = System.currentTimeMillis() + FALLBACK_RETRY_MS;
        }

        try {
            return chain.proceed(toLocal(request));
        } catch (IOException localError) {
            if (!triedTunnel && !chain.call().isCanceled()) {
                // Local is down while we were skipping the tunnel; try the tunnel now.
                useLocalUntil = 0L;
                return chain.proceed(request);
            }
            // Both are down: surface a connection error so offline mode kicks in.
            throw tunnelError != null ? tunnelError : localError;
        }
    }

    /** True when the reply is ngrok's own error page rather than the backend. */
    private static boolean isNgrokTunnelError(Response response) {
        return response.header("ngrok-error-code") != null;
    }

    /** Same request, pointed at the local backend. */
    private static Request toLocal(Request request) {
        HttpUrl url = request.url().newBuilder()
                .scheme(LOCAL_FALLBACK_URL.scheme())
                .host(LOCAL_FALLBACK_URL.host())
                .port(LOCAL_FALLBACK_URL.port())
                .build();
        return request.newBuilder().url(url).build();
    }

    /**
     * Skips the ngrok warning page and notes if the API is up.
     * Failures open the offline-report dialog.
     */
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

    /**
     * True for writes that change item or claim data.
     * Auth and FCM calls do not count.
     */
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

    /**
     * Shared API service, built on first use.
     *
     * @return the API service
     */
    public static synchronized ReClaimApiService getApiService() {
        if (apiService == null) {
            apiService = getInstance().create(ReClaimApiService.class);
        }
        return apiService;
    }
}
