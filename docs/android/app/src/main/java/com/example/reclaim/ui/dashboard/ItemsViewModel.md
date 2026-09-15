```java
public ItemsViewModel(@NonNull Application application) {
    super(application);
    apiService = RetrofitClient.getApiService();
}
```

What that function does: I store the API service on the application ViewModel.
What the function is responsible for: I am the ViewModel constructor Android creates.
Where it works: HomeFragment and SearchFragment get me from ViewModelProvider(requireActivity()).
Why I chose to write the code of the function the way its written: Sharing at activity scope means Home and Search use one list.
More: I extend AndroidViewModel so I have Application for strings and TokenManager.

```java
public LiveData<List<Item>> getItems() {
    return items;
}
```

What that function does: I return the shared item list.
What the function is responsible for: I let Home and Search observe items.
Where it works: I am observed by HomeFragment and SearchFragment.
Why I chose to write the code of the function the way its written: I return LiveData so only this class can set the list.
More: I wrap MutableLiveData items.

```java
public LiveData<Boolean> getLoading() {
    return loading;
}
```

What that function does: I return whether a fetch is running.
What the function is responsible for: I let screens show a spinner.
Where it works: I am observed by HomeFragment and SearchFragment.
Why I chose to write the code of the function the way its written: I return LiveData so only this class can set loading.
More: I wrap MutableLiveData loading.

```java
public LiveData<String> getErrorMessage() {
    return errorMessage;
}
```

What that function does: I return the last load-error text.
What the function is responsible for: I let screens toast a fetch failure.
Where it works: I am observed by HomeFragment and SearchFragment.
Why I chose to write the code of the function the way its written: I return LiveData so only this class can set the message.
More: I wrap MutableLiveData errorMessage.

```java
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
```

What that function does: I fetch all items and drop the signed-in user's own reports.
What the function is responsible for: I am the shared GET /api/items for Home and Search.
Where it works: I am called by HomeFragment and SearchFragment.
Why I chose to write the code of the function the way its written: Own reports belong on My Items. A network failure stays quiet so the offline dialog can speak.
More: I use TokenManager.getAuthHeader, apiService.getItems, and excludeOwnReports.

```java
public void onResponse(@NonNull Call<List<Item>> call,
                       @NonNull Response<List<Item>> response) {
    loading.setValue(false);
    if (response.isSuccessful() && response.body() != null) {
        items.setValue(excludeOwnReports(response.body()));
    } else if (NetworkMonitor.isBackendAvailable(getApplication())) {
        errorMessage.setValue(getApplication().getString(R.string.msg_failed_to_load_items));
    }
}
```

What that function does: I publish the filtered list, or I set an error if the API is up.
What the function is responsible for: I am the Retrofit success path for getItems.
Where it works: Retrofit calls me from loadItems.
Why I chose to write the code of the function the way its written: I skip the error toast when the backend is down.
More: I use NetworkMonitor.isBackendAvailable.

```java
public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
    loading.setValue(false);
}
```

What that function does: I clear loading and do not set an error.
What the function is responsible for: I am the Retrofit failure path for getItems.
Where it works: Retrofit calls me from loadItems.
Why I chose to write the code of the function the way its written: OfflineProtocol already shows the outage dialog.
More: I only set loading to false.

```java
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
```

What that function does: I drop items whose owner is the signed-in user.
What the function is responsible for: I keep Home and Search free of the user's own reports.
Where it works: I am called from loadItems on a successful reply.
Why I chose to write the code of the function the way its written: Comparing ownerId to TokenManager.getUserId is enough. A missing user id leaves the list as-is.
More: I use TokenManager.getUserId and Item.getOwnerId.
