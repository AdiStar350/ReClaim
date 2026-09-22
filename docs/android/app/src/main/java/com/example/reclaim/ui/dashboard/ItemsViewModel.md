# ItemsViewModel.java

## Overview
This is a **ViewModel** — a class that holds screen data and survives configuration changes like rotation (see `docs/GLOSSARY.md`). It loads the public list of items once and shares it, so both `HomeFragment` and `SearchFragment` read the same data instead of each fetching its own copy. It exposes the item list, a loading flag, and an error message as **LiveData** so the screens can observe changes.

## Concepts used here
- **ViewModel (`AndroidViewModel`)** — survives rotation and is shared by a screen's fragments. `AndroidViewModel` is a ViewModel that also has the `Application` context. See `docs/GLOSSARY.md`.
- **LiveData / MutableLiveData** — observable data holders; the UI reacts when the value changes. The private `Mutable` versions are edited here; the public read-only `LiveData` versions are exposed. See `docs/GLOSSARY.md`.
- **Retrofit `Call` / `enqueue`** — loads the items in the background. See `docs/GLOSSARY.md`.

## Libraries and imports
- **AndroidX Lifecycle**: `AndroidViewModel` (base class with app context), `LiveData` (read-only observable), `MutableLiveData` (writable observable).
- **Android framework**: `Application` is the app-wide context passed to the ViewModel.
- **Project classes**: `Item` (the data type), `RetrofitClient`/`ReClaimApiService` (API), `TokenManager` (reads the auth header and user id), `NetworkMonitor` (checks whether the backend is reachable before showing an error), and `R` (string resources).
- **Java standard**: `ArrayList`/`List` hold the items.
- **Retrofit**: `Call`, `Callback`, `Response`.

## Fields
- `private final ReClaimApiService apiService;` — makes the "get items" call; created in the constructor.
- `private final MutableLiveData<List<Item>> items;` — the current list of items (starts empty); edited here, exposed read-only.
- `private final MutableLiveData<Boolean> loading;` — true while a load is running, so screens can show a spinner.
- `private final MutableLiveData<String> errorMessage;` — the latest error text, observed to show a toast.

## Functions

### `public ItemsViewModel(Application application)` (constructor)
```java
public ItemsViewModel(@NonNull Application application) {
    super(application);
    apiService = RetrofitClient.getApiService();
}
```
- **What it does:** Builds the ViewModel and gets the shared API client.
- **Where it's called:** Android's `ViewModelProvider` creates it the first time a screen requests this ViewModel (in `HomeFragment` and `SearchFragment`).
- **Why it's written this way:** `AndroidViewModel` needs the `Application`, and creating `apiService` once here avoids rebuilding it per request.
- **Step by step:**
  1. Pass the `application` to the superclass.
  2. Store the shared API service.

### `public LiveData<List<Item>> getItems()` / `getLoading()` / `getErrorMessage()`
```java
public LiveData<List<Item>> getItems() {
    return items;
}

public LiveData<Boolean> getLoading() {
    return loading;
}

public LiveData<String> getErrorMessage() {
    return errorMessage;
}
```
- **What it does:** Hand out read-only views of the three data holders so screens can observe them.
- **Where it's called:** `HomeFragment.observeItems` and `SearchFragment.onViewCreated` observe these to update the list, spinner, and error toasts.
- **Why it's written this way:** They return `LiveData` (not `MutableLiveData`) so outside classes can read and observe but only the ViewModel can change the values — a clean separation.
- **Step by step:**
  1. Each getter simply returns its matching field as the read-only `LiveData` type.

### `private List<Item> excludeOwnReports(List<Item> source)`
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
- **What it does:** Removes the current user's own reports from a list, so they don't appear on Home or Search.
- **Where it's called:** From `loadItems`, on the successful response before the list is published.
- **Why it's written this way:** A user's own items belong on the "My Items" screen, not mixed into browsing. If the user id is unknown, the list is returned unchanged rather than risking hiding everything.
- **Step by step:**
  1. Read the current user id; if missing, return the source list unchanged.
  2. Walk the list and keep only items whose owner id is not the current user.
  3. Return the filtered list.

### `public void loadItems()`
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
- **What it does:** Loads the item list from the backend, filters out the user's own reports, and publishes the result (or an error) through the LiveData holders.
- **Where it's called:** From `HomeFragment.refreshFromServer` and `SearchFragment` (on resume, on pull-to-refresh, and when app data changes).
- **Why it's written this way:** It only shows a "failed to load" error when the backend is actually reachable (`NetworkMonitor`), so a normal offline state does not spam an error — the offline banner handles that. Updating LiveData means every observing screen refreshes automatically.
- **Step by step:**
  1. Read the auth header; if the user is not signed in, set the "not authenticated" error and stop.
  2. Set `loading` to true.
  3. `enqueue` `apiService.getItems(authHeader)`.
  4. In `onResponse`: set `loading` false; on success publish the filtered list; on an error response, set the "failed to load" message only if the backend is reachable.
  5. In `onFailure`: set `loading` false (no error message, since this usually means offline).
