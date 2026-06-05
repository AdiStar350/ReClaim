package com.example.reclaim.network;

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
     * {@code 10.0.2.2} is the special alias that the Android emulator
     * uses to reach the host machine's {@code localhost}. Replace with
     * your production URL before release.
     * </p>
     */
    private static final String BASE_URL = "http://10.0.2.2:8080/";

    /** The single Retrofit instance. */
    private static Retrofit retrofit;

    /** The single API service instance. */
    private static ReClaimApiService apiService;

    // Prevent instantiation
    private RetrofitClient() {
    }

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
                    .addInterceptor(loggingInterceptor)
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
