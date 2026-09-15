```java
public PlacesAutocompleteAdapter(@NonNull Context context,
                                 @NonNull PlacesClient placesClient) {
    super(context, R.layout.item_places_dropdown, new ArrayList<>());
    this.placesClient = placesClient;
    setDropDownViewResource(R.layout.item_places_dropdown);
}
```

What that function does: I store the Places client and use the light dropdown row layout.
What the function is responsible for: I am the adapter constructor.
Where it works: ReportActivity constructs me. Rows use item_places_dropdown.xml.
Why I chose to write the code of the function the way its written: The same layout for getView and the dropdown keeps text dark on light.
More: I extend ArrayAdapter<String> and keep PlacesClient.

```java
public void bindField(@NonNull AutoCompleteTextView field) {
    fieldRef = new WeakReference<>(field);
}
```

What that function does: I remember the text field so I can show the dropdown after results arrive.
What the function is responsible for: I keep a weak link to the AutoCompleteTextView.
Where it works: ReportActivity calls me after constructing the adapter.
Why I chose to write the code of the function the way its written: A WeakReference avoids leaking the activity if a Places reply is late.
More: I use java.lang.ref.WeakReference.

```java
@Nullable
public AutocompletePrediction getPrediction(int position) {
    return position >= 0 && position < predictions.size()
            ? predictions.get(position) : null;
}
```

What that function does: I return the Places prediction at that row, or null.
What the function is responsible for: I let ReportActivity read placeId after a row tap.
Where it works: ReportActivity and getItem call me.
Why I chose to write the code of the function the way its written: Bounds checks stop a crash on a stale click.
More: I return AutocompletePrediction.

```java
@NonNull
public AutocompleteSessionToken getSessionToken() {
    return sessionToken;
}
```

What that function does: I return the current Places session token.
What the function is responsible for: I let ReportActivity reuse the session when fetching place details.
Where it works: ReportActivity reads me after a suggestion is picked.
Why I chose to write the code of the function the way its written: One token per search session is cheaper in Places billing.
More: I return AutocompleteSessionToken.

```java
public void resetSessionToken() {
    sessionToken = AutocompleteSessionToken.newInstance();
}
```

What that function does: I start a new Places session after a place is picked.
What the function is responsible for: I close the old autocomplete session.
Where it works: ReportActivity calls me after a place details fetch.
Why I chose to write the code of the function the way its written: A new token starts the next search clean.
More: I use AutocompleteSessionToken.newInstance.

```java
@Override
public int getCount() {
    return predictions.size();
}
```

What that function does: I return how many suggestions I hold.
What the function is responsible for: I tell the dropdown the list size.
Where it works: ArrayAdapter and the AutoCompleteTextView call me.
Why I chose to write the code of the function the way its written: Size follows the Places list, not the empty super list.
More: I return predictions.size().

```java
@Nullable
@Override
public String getItem(int position) {
    AutocompletePrediction prediction = getPrediction(position);
    return prediction != null ? prediction.getFullText(null).toString() : null;
}
```

What that function does: I return the full address text for that row.
What the function is responsible for: I am the string the field shows when a row is picked.
Where it works: AutoCompleteTextView calls me.
Why I chose to write the code of the function the way its written: getFullText is the human address. placeId stays on the prediction object.
More: I use AutocompletePrediction.getFullText.

```java
@NonNull
@Override
public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
    return styleDropdownRow(super.getView(position, convertView, parent));
}
```

What that function does: I style the selected-item view.
What the function is responsible for: I keep row colors readable.
Where it works: ArrayAdapter calls me.
Why I chose to write the code of the function the way its written: super builds the TextView from item_places_dropdown. I only tint it.
More: I call styleDropdownRow.

```java
@NonNull
@Override
public View getDropDownView(int position, @Nullable View convertView,
                            @NonNull ViewGroup parent) {
    return styleDropdownRow(super.getDropDownView(position, convertView, parent));
}
```

What that function does: I style each dropdown row.
What the function is responsible for: I keep suggestion rows dark-on-light.
Where it works: AutoCompleteTextView calls me for the popup.
Why I chose to write the code of the function the way its written: Both getView and getDropDownView share styleDropdownRow.
More: I call styleDropdownRow.

```java
@NonNull
private View styleDropdownRow(@NonNull View view) {
    view.setBackgroundColor(ContextCompat.getColor(
            getContext(), R.color.md_theme_surfaceContainerLowest));
    if (view instanceof TextView) {
        ((TextView) view).setTextColor(
                ContextCompat.getColor(getContext(), R.color.brand_dark));
    }
    return view;
}
```

What that function does: I force a light background and dark text on the row.
What the function is responsible for: I fix theme colors that would make white text on white.
Where it works: I am called from getView and getDropDownView.
Why I chose to write the code of the function the way its written: Night theme can invert dropdown text. I pin brand_dark.
More: I use ContextCompat.getColor.

```java
@NonNull
@Override
public Filter getFilter() {
    return new Filter() {
        @Override
        protected FilterResults performFiltering(@Nullable CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.length() < MIN_QUERY_LENGTH) {
                requestPredictions(null);
                results.count = 0;
                return results;
            }
            requestPredictions(constraint.toString());
            results.count = 0;
            return results;
        }

        @Override
        protected void publishResults(@Nullable CharSequence constraint,
                                      @Nullable FilterResults results) {
            // The Places listeners update the list on the main thread.
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return resultValue instanceof AutocompletePrediction
                    ? ((AutocompletePrediction) resultValue).getFullText(null)
                    : super.convertResultToString(resultValue);
        }
    };
}
```

What that function does: I start a Places lookup when the user types at least 3 letters.
What the function is responsible for: I am the AutoComplete filter. I do not return rows here.
Where it works: AutoCompleteTextView calls me as the user types. ReportActivity hosts the field.
Why I chose to write the code of the function the way its written: Places is async, so publishResults is empty. applyPredictions updates the list later. Short queries clear suggestions.
More: I use MIN_QUERY_LENGTH = 3 and requestPredictions.

```java
protected FilterResults performFiltering(@Nullable CharSequence constraint) {
    FilterResults results = new FilterResults();
    if (constraint == null || constraint.length() < MIN_QUERY_LENGTH) {
        requestPredictions(null);
        results.count = 0;
        return results;
    }
    requestPredictions(constraint.toString());
    results.count = 0;
    return results;
}
```

What that function does: I kick off Places, or I clear if the query is short.
What the function is responsible for: I am the background filter step.
Where it works: Filter calls me off the main thread.
Why I chose to write the code of the function the way its written: results.count stays 0 because Places fills the list later.
More: I call requestPredictions.

```java
protected void publishResults(@Nullable CharSequence constraint,
                              @Nullable FilterResults results) {
    // The Places listeners update the list on the main thread.
}
```

What that function does: I do nothing here.
What the function is responsible for: I fill the Filter API.
Where it works: Filter calls me on the main thread after performFiltering.
Why I chose to write the code of the function the way its written: applyPredictions already calls notifyDataSetChanged.
More: I leave the body as a comment.

```java
public CharSequence convertResultToString(Object resultValue) {
    return resultValue instanceof AutocompletePrediction
            ? ((AutocompletePrediction) resultValue).getFullText(null)
            : super.convertResultToString(resultValue);
}
```

What that function does: I turn a prediction into address text for the field.
What the function is responsible for: I am the Filter string conversion.
Where it works: AutoCompleteTextView may call me when filling the field.
Why I chose to write the code of the function the way its written: Predictions are not Strings, so I use getFullText.
More: I check instanceof AutocompletePrediction.

```java
private void requestPredictions(@Nullable String query) {
    int generation = requestGeneration.incrementAndGet();
    if (query == null) {
        applyPredictions(generation, new ArrayList<>());
        return;
    }

    FindAutocompletePredictionsRequest request =
            FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .setSessionToken(sessionToken)
                    .build();

    placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener(response -> applyPredictions(
                    generation, response.getAutocompletePredictions()))
            .addOnFailureListener(e -> {
                logPlacesFailure(e);
                applyPredictions(generation, new ArrayList<>());
            });
}
```

What that function does: I ask Places for suggestions, and I ignore stale replies with a generation number.
What the function is responsible for: I am the Places HTTP/SDK call.
Where it works: I am called from performFiltering.
Why I chose to write the code of the function the way its written: Fast typing would otherwise show an old list. incrementAndGet drops older generations.
More: I use PlacesClient.findAutocompletePredictions and AtomicInteger.

```java
private void applyPredictions(int generation,
                              @NonNull List<AutocompletePrediction> found) {
    if (generation != requestGeneration.get()) {
        return;
    }
    Runnable update = () -> {
        if (generation != requestGeneration.get()) {
            return;
        }
        predictions = new ArrayList<>(found);
        notifyDataSetChanged();
        AutoCompleteTextView field = fieldRef != null ? fieldRef.get() : null;
        if (field != null && field.isFocused() && !predictions.isEmpty()) {
            field.showDropDown();
        }
    };
    if (Looper.myLooper() == Looper.getMainLooper()) {
        update.run();
    } else {
        mainHandler.post(update);
    }
}
```

What that function does: I replace the suggestion list on the UI thread and show the dropdown.
What the function is responsible for: I am the Places success/clear update.
Where it works: I am called from requestPredictions.
Why I chose to write the code of the function the way its written: Places callbacks may not be on main. I only show the dropdown if the field still has focus.
More: I use Handler(Looper.getMainLooper) and notifyDataSetChanged.

```java
private void logPlacesFailure(@NonNull Exception e) {
    if (e instanceof ApiException) {
        ApiException api = (ApiException) e;
        int statusCode = api.getStatusCode();
        Log.e(TAG, "Places autocomplete failed. statusCode=" + statusCode
                + " status=" + PlacesStatusCodes.getStatusCodeString(statusCode)
                + " message=" + api.getMessage(), e);
        return;
    }
    Log.e(TAG, "Places autocomplete failed", e);
}
```

What that function does: I log a Places failure with status code when I can.
What the function is responsible for: I make billing/key errors easier to read in logcat.
Where it works: I am called from requestPredictions's failure listener.
Why I chose to write the code of the function the way its written: ApiException has a Places status string. Other errors just log the throwable.
More: I use PlacesStatusCodes.getStatusCodeString.
