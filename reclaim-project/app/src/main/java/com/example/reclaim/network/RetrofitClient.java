package com.example.reclaim.network;

import com.example.reclaim.data.AppDataRefresh;
import com.example.reclaim.sync.BackendAvailability;
import com.example.reclaim.sync.OfflineProtocol;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
                    AppDataRefresh.notifyChanged();
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
