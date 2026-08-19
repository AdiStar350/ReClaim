package com.example.reclaim.ui.report;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.reclaim.R;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Adapter that feeds Google Places autocomplete predictions into an
 * {@link android.widget.AutoCompleteTextView}.
 * <p>
 * The framework invokes {@link Filter#performFiltering} on a background
 * thread, where this adapter queries the Places API synchronously (with a
 * timeout) and publishes the resulting predictions to the dropdown.
 * A session token groups keystrokes into one billing session; call
 * {@link #resetSessionToken()} after a place is fetched.
 * </p>
 */
public class PlacesAutocompleteAdapter extends ArrayAdapter<String> {

    private static final int QUERY_TIMEOUT_SECONDS = 5;
    private static final int MIN_QUERY_LENGTH = 3;

    private final PlacesClient placesClient;
    private List<AutocompletePrediction> predictions = new ArrayList<>();
    private AutocompleteSessionToken sessionToken = AutocompleteSessionToken.newInstance();

    public PlacesAutocompleteAdapter(@NonNull Context context,
                                     @NonNull PlacesClient placesClient) {
        super(context, R.layout.item_dropdown_category, new ArrayList<>());
        this.placesClient = placesClient;
    }

    /** Returns the prediction backing the given dropdown row. */
    @Nullable
    public AutocompletePrediction getPrediction(int position) {
        return position >= 0 && position < predictions.size()
                ? predictions.get(position) : null;
    }

    @NonNull
    public AutocompleteSessionToken getSessionToken() {
        return sessionToken;
    }

    /** Starts a new billing session; call after fetching a selected place. */
    public void resetSessionToken() {
        sessionToken = AutocompleteSessionToken.newInstance();
    }

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

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(@Nullable CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint == null || constraint.length() < MIN_QUERY_LENGTH) {
                    results.values = new ArrayList<AutocompletePrediction>();
                    results.count = 0;
                    return results;
                }

                FindAutocompletePredictionsRequest request =
                        FindAutocompletePredictionsRequest.builder()
                                .setQuery(constraint.toString())
                                .setSessionToken(sessionToken)
                                .build();
                try {
                    FindAutocompletePredictionsResponse response = Tasks.await(
                            placesClient.findAutocompletePredictions(request),
                            QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    List<AutocompletePrediction> found =
                            response.getAutocompletePredictions();
                    results.values = found;
                    results.count = found.size();
                } catch (Exception e) {
                    // Network/API failure — show no suggestions
                    results.values = new ArrayList<AutocompletePrediction>();
                    results.count = 0;
                }
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(@Nullable CharSequence constraint,
                                          @Nullable FilterResults results) {
                predictions = results != null && results.values != null
                        ? (List<AutocompletePrediction>) results.values
                        : new ArrayList<>();
                if (predictions.isEmpty()) {
                    notifyDataSetInvalidated();
                } else {
                    notifyDataSetChanged();
                }
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                return resultValue instanceof AutocompletePrediction
                        ? ((AutocompletePrediction) resultValue).getFullText(null)
                        : super.convertResultToString(resultValue);
            }
        };
    }
}
