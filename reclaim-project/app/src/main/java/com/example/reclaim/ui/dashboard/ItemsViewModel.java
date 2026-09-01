package com.example.reclaim.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.reclaim.model.Item;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.sync.NetworkMonitor;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity-scoped ViewModel that loads and shares the item list
 * between {@link ListFragment} and {@link MapFragment}.
 */
public class ItemsViewModel extends AndroidViewModel {

    private final ReClaimApiService apiService;
    private final MutableLiveData<List<Item>> items = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ItemsViewModel(@NonNull Application application) {
        super(application);
        apiService = RetrofitClient.getApiService();
    }

    public LiveData<List<Item>> getItems() {
        return items;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadItems() {
        String authHeader = TokenManager.getAuthHeader(getApplication());
        if (authHeader == null) {
            errorMessage.setValue("Not authenticated");
            return;
        }

        loading.setValue(true);
        apiService.getItems(authHeader).enqueue(new Callback<List<Item>>() {
            @Override
            public void onResponse(@NonNull Call<List<Item>> call,
                                   @NonNull Response<List<Item>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    items.setValue(excludeOwnReports(response.body()));
                } else {
                    errorMessage.setValue("Failed to load items");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
                loading.setValue(false);
                // Suppress connection-error alerts while offline; the app shows a
                // single global offline dialog on launch instead.
                if (NetworkMonitor.isOnline(getApplication())) {
                    errorMessage.setValue("Network error while loading items");
                }
            }
        });
    }

    /**
     * The viewer manages their own open reports on My Items, not on the
     * shared home/search feeds.
     */
    private List<Item> excludeOwnReports(List<Item> source) {
        String userId = TokenManager.getUserId(getApplication());
        if (userId == null || userId.isEmpty()) {
            return source;
        }
        List<Item> filtered = new ArrayList<>();
        for (Item item : source) {
            if (!userId.equals(item.getOwnerId())) {
                filtered.add(item);
            }
        }
        return filtered;
    }
}
