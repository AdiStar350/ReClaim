package com.example.reclaim.ui.report;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.reclaim.R;
import com.google.android.gms.common.api.ApiException;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.PlacesStatusCodes;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Suggests addresses as the user types.
 * Call {@link #resetSessionToken()} after a place is picked.
 */
public class PlacesAutocompleteAdapter extends ArrayAdapter<String> {

    private static final String TAG = "PlacesAutocomplete";
    private static final int MIN_QUERY_LENGTH = 3;

    private final PlacesClient placesClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger requestGeneration = new AtomicInteger();
    private List<AutocompletePrediction> predictions = new ArrayList<>();
    private AutocompleteSessionToken sessionToken = AutocompleteSessionToken.newInstance();
    @Nullable
    private WeakReference<AutoCompleteTextView> fieldRef;

    public PlacesAutocompleteAdapter(@NonNull Context context,
                                     @NonNull PlacesClient placesClient) {
        super(context, R.layout.item_places_dropdown, new ArrayList<>());
        this.placesClient = placesClient;
        setDropDownViewResource(R.layout.item_places_dropdown);
    }

    public void bindField(@NonNull AutoCompleteTextView field) {
        fieldRef = new WeakReference<>(field);
    }

    /** The suggestion at this row. */
    @Nullable
    public AutocompletePrediction getPrediction(int position) {
        return position >= 0 && position < predictions.size()
                ? predictions.get(position) : null;
    }

    @NonNull
    public AutocompleteSessionToken getSessionToken() {
        return sessionToken;
    }

    /** Call after a place is picked. */
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
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return styleDropdownRow(super.getView(position, convertView, parent));
    }

    @NonNull
    @Override
    public View getDropDownView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
        return styleDropdownRow(super.getDropDownView(position, convertView, parent));
    }

    /**
     * Keeps row text dark on a light background.
     */
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

    /**
     * Replaces the suggestion list. Always runs on the UI thread.
     */
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
}
