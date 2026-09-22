# PlacesAutocompleteAdapter.java

## Overview
This is an `ArrayAdapter` for address suggestions while the user types on the report form. It talks to the **Google Places** Autocomplete API (see `docs/GLOSSARY.md`), debounces overlapping requests with a generation counter, and shows a styled dropdown under an `AutoCompleteTextView`.

## Concepts used here
- **ArrayAdapter / Filter** — Android autocomplete plumbing; `getFilter` kicks off Places queries as the user types.
- **Google Places** — `PlacesClient.findAutocompletePredictions` returns `AutocompletePrediction` rows. See `docs/GLOSSARY.md`.
- **Session token** — groups keystrokes into one Places billing session; reset after a place is picked.
- **WeakReference** — holds the AutoCompleteTextView without leaking it.
- **Handler / main looper** — applies prediction results on the UI thread.

## Libraries and imports
- **Android framework**: ArrayAdapter, Filter, Handler, Looper, AutoCompleteTextView, TextView.
- **AndroidX**: ContextCompat for colors.
- **Google Places**: PlacesClient, AutocompletePrediction, AutocompleteSessionToken, FindAutocompletePredictionsRequest, PlacesStatusCodes, ApiException.
- **Project**: dropdown layout / colors.

## Fields
- `private final PlacesClient placesClient` — Places API client from ReportActivity.
- `private final Handler mainHandler` — posts UI updates to the main thread.
- `private final AtomicInteger requestGeneration` — increments per request so stale replies are ignored.
- `private List<AutocompletePrediction> predictions` — current suggestion list.
- `private AutocompleteSessionToken sessionToken` — current Places session.
- `private WeakReference<AutoCompleteTextView> fieldRef` — optional field to show the dropdown on.

## Functions

### `public PlacesAutocompleteAdapter(Context context, PlacesClient placesClient)`
```java
public PlacesAutocompleteAdapter(@NonNull Context context,
                                 @NonNull PlacesClient placesClient) {
    super(context, R.layout.item_places_dropdown, new ArrayList<>());
    this.placesClient = placesClient;
    setDropDownViewResource(R.layout.item_places_dropdown);
}
```
- **What it does:** Builds the adapter with the places dropdown layout and stores the client.
- **Where it's called:** `ReportActivity` when setting up location autocomplete.
- **Why it's written this way:** Same layout for item and dropdown rows.
- **Step by step:**
  1. Call super with empty list and dropdown layout.
  2. Store `placesClient`; set dropdown view resource.

### `public void bindField(AutoCompleteTextView field)`
```java
public void bindField(@NonNull AutoCompleteTextView field) {
    fieldRef = new WeakReference<>(field);
}
```
- **What it does:** Remembers the text field so results can call `showDropDown()` when focused.
- **Where it's called:** From `ReportActivity` after creating the adapter.
- **Why it's written this way:** WeakReference avoids holding the view if the Activity is gone.
- **Step by step:**
  1. Wrap `field` in a WeakReference.

### `public AutocompletePrediction getPrediction(int position)` / `getSessionToken()` / `resetSessionToken()`
```java
@Nullable
public AutocompletePrediction getPrediction(int position) { /* bounds check */ }

@NonNull
public AutocompleteSessionToken getSessionToken() {
    return sessionToken;
}

public void resetSessionToken() {
    sessionToken = AutocompleteSessionToken.newInstance();
}
```
- **What it does:** Returns the prediction at a row; exposes or renews the session token.
- **Where it's called:** `ReportActivity` when the user picks a suggestion (prediction + session for place details); `resetSessionToken` after a successful pick (per class Javadoc).
- **Why it's written this way:** ReportActivity needs the full prediction object (not just the display string) and a fresh session after selection for Places billing rules.
- **Step by step:**
  1. `getPrediction`: return list entry or null if out of range.
  2. `getSessionToken`: return current token.
  3. `resetSessionToken`: create a new token instance.

### `getCount` / `getItem` / `getView` / `getDropDownView` / `styleDropdownRow`
```java
@Override
public int getCount() {
    return predictions.size();
}

@Nullable
@Override
public String getItem(int position) {
    AutocompletePrediction prediction = getPrediction(position);
    return prediction != null ? prediction.getFullText(null).toString() : null;
}

// getView / getDropDownView → styleDropdownRow(super...)
```
- **What it does:** Reports count from predictions; item text is full prediction text; styles rows dark-on-light.
- **Where it's called:** By the AutoCompleteTextView / ArrayAdapter framework when drawing the dropdown.
- **Why it's written this way:** Super draws the TextView; `styleDropdownRow` forces brand colors so the dropdown stays readable.
- **Step by step:**
  1. Count = predictions size; item = full text.
  2. Style background and TextView color on each row.

### `public Filter getFilter()`
```java
@NonNull
@Override
public Filter getFilter() {
    return new Filter() {
        @Override
        protected FilterResults performFiltering(@Nullable CharSequence constraint) {
            // if query < 3 chars → clear; else requestPredictions; results.count = 0
        }
        @Override
        protected void publishResults(...) {
            // Places listeners update the list on the main thread.
        }
        // convertResultToString for AutocompletePrediction
    };
}
```
- **What it does:** On each keystroke (via Filter), starts a Places request if the query is at least 3 characters; otherwise clears suggestions. Does not publish through FilterResults — Places callbacks update the list.
- **Where it's called:** Automatically by AutoCompleteTextView as the user types.
- **Why it's written this way:** Places is async; returning count 0 from `performFiltering` avoids the Filter publishing stale sync results. `MIN_QUERY_LENGTH` reduces noise/API cost.
- **Step by step:**
  1. If constraint null or shorter than 3: `requestPredictions(null)` and return empty results.
  2. Else `requestPredictions(query)`; return results with count 0.
  3. `publishResults` is intentionally empty.
  4. `convertResultToString` uses prediction full text when needed.

### `private void requestPredictions(String query)` / `applyPredictions` / `logPlacesFailure`
```java
private void requestPredictions(@Nullable String query) {
    int generation = requestGeneration.incrementAndGet();
    if (query == null) {
        applyPredictions(generation, new ArrayList<>());
        return;
    }
    // FindAutocompletePredictionsRequest → success/failure → applyPredictions
}

private void applyPredictions(int generation, List<AutocompletePrediction> found) {
    // ignore stale generation; update predictions; notify; maybe showDropDown
}
```
- **What it does:** Fires a Places autocomplete request tagged with a generation; applies only the latest generation’s results on the main thread; logs API failures.
- **Where it's called:** From the Filter’s `performFiltering`; Places listeners call `applyPredictions`.
- **Why it's written this way:** Fast typing can overlap requests; generation ignores outdated replies. Clearing with null query empties the list. Showing the dropdown only when focused avoids popups at the wrong time.
- **Step by step:**
  1. Bump generation; if query null, apply empty list.
  2. Build request with query + session token; call `findAutocompletePredictions`.
  3. On success/failure: `applyPredictions` (failure → empty + log).
  4. `applyPredictions`: skip if generation stale; set `predictions`, `notifyDataSetChanged`, maybe `showDropDown`.
