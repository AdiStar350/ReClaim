package com.example.reclaim.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.reclaim.R;
import com.example.reclaim.databinding.FragmentMapBinding;
import com.example.reclaim.model.Item;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment that displays item locations on a Google Map.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final LatLng DEFAULT_LOCATION = new LatLng(32.0853, 34.7818);

    private FragmentMapBinding binding;
    private GoogleMap googleMap;
    private ItemsViewModel viewModel;
    private final Map<Marker, Item> markerItems = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ItemsViewModel.class);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        viewModel.getItems().observe(getViewLifecycleOwner(), this::renderMarkers);

        if (viewModel.getItems().getValue() == null
                || viewModel.getItems().getValue().isEmpty()) {
            viewModel.loadItems();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        }

        googleMap.setOnMarkerClickListener(marker -> {
            Item item = markerItems.get(marker);
            if (item != null) {
                startActivity(ItemNavigationHelper.createDetailsIntent(requireContext(), item));
            }
            return false;
        });

        List<Item> currentItems = viewModel.getItems().getValue();
        if (currentItems != null) {
            renderMarkers(currentItems);
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 12f));
        }
    }

    private void renderMarkers(List<Item> items) {
        if (googleMap == null || items == null) {
            return;
        }

        googleMap.clear();
        markerItems.clear();

        LatLng firstPosition = null;
        for (Item item : items) {
            if (item.getLatitude() == null || item.getLongitude() == null) {
                continue;
            }

            LatLng position = new LatLng(item.getLatitude(), item.getLongitude());
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(item.getTitle())
                    .snippet(item.getLocation()));
            if (marker != null) {
                markerItems.put(marker, item);
            }

            if (firstPosition == null) {
                firstPosition = position;
            }
        }

        if (firstPosition != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstPosition, 12f));
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 12f));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
