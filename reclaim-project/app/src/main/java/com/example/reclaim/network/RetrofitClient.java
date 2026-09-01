package com.example.reclaim.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton provider for the configured {@link Retrofit} instance and
 * the {@link ReClaimApiService} API interface.
 * <p>
 * Uses a placeholder {@code BASE_URL} pointing to {@code 10.0.2.2:8080}
 * (the Android emulator's alias for the host machine's localhost).
 * An {@link HttpLoggingInterceptor} is attached for debugging HTTP
 * request/response details in Logcat.
 * </p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * ReClaimApiService api = RetrofitClient.getApiService();
 * api.login(new LoginRequest("user@email.com", "pass")).enqueue(...);
 * }</pre>
 */
public final class RetrofitClient {

    /**
     * Base URL for the ReClaim backend API.
     * <p>
     * To work from ANY network (different Wi-Fi, cellular, etc.) point this at
     * your ngrok static domain, which stays constant across locations, e.g.:
     * {@code "https://your-name.ngrok-free.app/"}. Start the tunnel with
     * {@code reclaim-backend/tunnel.sh} (or
     * {@code ngrok http --url=your-name.ngrok-free.app 8080}).
     * <p>
     * For local-only testing you can instead use your Mac's LAN IP
     * ({@code http://10.100.102.32:8080/}) or the emulator alias
     * ({@code http://10.0.2.2:8080/}). The trailing slash is required.
     */
    private static final String BASE_URL = "https://decal-magnetism-coherent.ngrok-free.dev/";
    /** The single Retrofit instance. */
    private static Retrofit retrofit;

    /** The single API service instance. */
    private static ReClaimApiService apiService;

    // Prevent instantiation
    private RetrofitClient() { }

    /**
     * Returns the singleton {@link Retrofit} instance, creating it on
     * the first call with an OkHttp client that includes a body-level
     * logging interceptor.
     *
     * @return the configured {@link Retrofit} instance
     */
    public static synchronized Retrofit getInstance() {
        if (retrofit == null) {
            // Logging interceptor — logs full request/response bodies in debug
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    // ngrok's free tier serves a browser interstitial for
                    // "browser-like" requests; this header skips it so API
                    // calls go straight through. Harmless on non-ngrok hosts.
                    .addInterceptor(chain -> chain.proceed(
                            chain.request().newBuilder()
                                    .header("ngrok-skip-browser-warning", "true")
                                    .build()))
                    .addInterceptor(loggingInterceptor)
                    // Tunnels/cellular can be slower than LAN — be generous.
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
     * Returns the singleton {@link ReClaimApiService} implementation,
     * creating it from the Retrofit instance on the first call.
     *
     * @return the API service ready for making calls
     */
    public static synchronized ReClaimApiService getApiService() {
        if (apiService == null) {
            apiService = getInstance().create(ReClaimApiService.class);
        }
        return apiService;
    }
}
