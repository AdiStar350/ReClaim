package com.example.reclaim.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.reclaim.R;
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
 * Keeps the list of items so both Home and Search can use it.
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
            errorMessage.setValue(getApplication().getString(R.string.msg_not_authenticated));
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
                } else if (NetworkMonitor.isBackendAvailable(getApplication())) {
                    errorMessage.setValue(getApplication().getString(R.string.msg_failed_to_load_items));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
                loading.setValue(false);
            }
        });
    }

    /**
     * Own reports stay on My Items, not on Home or Search.
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
