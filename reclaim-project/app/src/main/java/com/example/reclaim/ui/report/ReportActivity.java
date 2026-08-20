package com.example.reclaim.ui.report;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.reclaim.R;
import com.example.reclaim.data.PendingReportStore;
import com.example.reclaim.databinding.ActivityReportBinding;
import com.example.reclaim.model.Item;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.storage.ImageUploadService;
import com.example.reclaim.sync.NetworkMonitor;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Screen for submitting a new lost or found item report.
 * <p>
 * Provides fields for title, description, category selection, photo
 * capture (camera or gallery via {@link ActivityResultLauncher}), and
 * an embedded {@link SupportMapFragment} for location picking. The user
 * can also tap "Use Current Location" to auto-fill the address field
 * using {@link FusedLocationProviderClient} and {@link Geocoder}.
 * </p>
 */
public class ReportActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "ReportActivity";

    public static final String EXTRA_ITEM_ID = "extra_item_id";

    /** Categories available for the item report dropdown. */
    private static final String[] CATEGORIES = {
            "Electronics", "Documents", "Keys", "Wallets",
            "Clothing", "Bags", "Other"
    };

    /** Default map zoom level. */
    private static final float DEFAULT_ZOOM = 15f;

    /** Default map center (Tel Aviv). */
    private static final LatLng DEFAULT_LOCATION = new LatLng(32.0853, 34.7818);

    private ActivityReportBinding binding;
    private ReClaimApiService apiService;
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();

    // ── Image capture state ──────────────────────────────────────────────
    /** URI of the photo captured by the camera (temp file via FileProvider). */
    private Uri cameraImageUri;

    /** URI of the currently selected/captured image for the report. */
    private Uri selectedImageUri;

    // ── Map / Location state ─────────────────────────────────────────────
    private GoogleMap googleMap;
    private Marker currentMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng selectedLatLng;
    @androidx.annotation.Nullable
    private PlacesAutocompleteAdapter placesAdapter;
    @androidx.annotation.Nullable
    private PlacesClient placesClient;
    @androidx.annotation.Nullable
    private String editingItemId;
    @androidx.annotation.Nullable
    private String existingImageUrl;

    // ═════════════════════════════════════════════════════════════════════
    //  ACTIVITY RESULT LAUNCHERS (modern replacement for startActivityForResult)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Launcher for {@link ActivityResultContracts.TakePicture}.
     * Receives {@code true} if the camera saved an image to {@link #cameraImageUri}.
     */
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) {
                    selectedImageUri = cameraImageUri;
                    displaySelectedImage(selectedImageUri);
                }
            });

    /**
     * Launcher for {@link ActivityResultContracts.GetContent}.
     * Returns the content URI of the image the user selected from the gallery.
     */
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    displaySelectedImage(uri);
                }
            });

    /**
     * Launcher that requests the {@link Manifest.permission#CAMERA} permission.
     * If granted, proceeds to launch the camera.
     */
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(this,
                            R.string.msg_camera_permission_required,
                            Toast.LENGTH_SHORT).show();
                }
            });

    /**
     * Launcher that requests location permissions (fine + coarse).
     * If at least one is granted, proceeds to fetch the current location.
     */
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                        boolean fineGranted = Boolean.TRUE.equals(
                                result.get(Manifest.permission.ACCESS_FINE_LOCATION));

                        boolean coarseGranted = Boolean.TRUE.equals(
                                result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                        if (fineGranted || coarseGranted) {
                            fetchCurrentLocation();
                        } else {
                            Toast.makeText(this,
                                    R.string.msg_location_permission_required,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

    // ═════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        apiService = RetrofitClient.getApiService();

        setupToolbar();
        setupCategoryDropdown();
        setupImageCapture();
        setupLocationPicker();
        setupReportTypeChips();
        setupSubmitButton();

        editingItemId = getIntent().getStringExtra(EXTRA_ITEM_ID);

        if (editingItemId != null) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.report_edit_title);
            }

            loadExistingItem(editingItemId);
        }
    }

    private void loadExistingItem(String itemId) {
        String authHeader = TokenManager.getAuthHeader(this);

        if (authHeader == null) {
            return;
        }

        apiService.getItemById(authHeader, itemId).enqueue(new Callback<Item>() {
            @Override
            public void onResponse(@NonNull Call<Item> call, @NonNull Response<Item> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ReportActivity.this,
                            R.string.error_generic, Toast.LENGTH_SHORT).show();
                    return;
                }
                populateForm(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<Item> call, @NonNull Throwable t) {
                Toast.makeText(ReportActivity.this,
                        R.string.error_generic, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateForm(@NonNull Item item) {
        binding.editItemTitle.setText(item.getTitle());
        binding.editDescription.setText(item.getDescription());
        binding.dropdownCategory.setText(item.getCategory(), false);
        binding.editLocation.setText(item.getLocation(), false);
        binding.editVerification.setText(item.getVerificationQuestion());

        if ("Lost".equalsIgnoreCase(item.getType())) {
            binding.chipTypeLost.setChecked(true);
        } else {
            binding.chipTypeFound.setChecked(true);
        }
        updateVerificationVisibility();

        existingImageUrl = item.getImageUrl();

        if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
            Glide.with(this).load(existingImageUrl).into(binding.imagePreview);
        }

        if (item.getLatitude() != null && item.getLongitude() != null) {
            selectedLatLng = new LatLng(item.getLatitude(), item.getLongitude());

            if (googleMap != null) {
                placeMarker(selectedLatLng);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, DEFAULT_ZOOM));
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  TOOLBAR
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Configures the Material toolbar with a back/up navigation button.
     */
    private void setupToolbar() {
        setSupportActionBar(binding.toolbarReport);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CATEGORY DROPDOWN
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Populates the category {@link android.widget.AutoCompleteTextView}
     * with a predefined list of item categories.
     */
    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.item_dropdown_category,
                CATEGORIES
        );
        binding.dropdownCategory.setAdapter(adapter);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  IMAGE CAPTURE (Camera + Gallery via ActivityResultLauncher)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Wires up click listeners on the image card and the "Choose from Gallery"
     * button. Tapping the card shows a Material dialog offering Camera or Gallery.
     */
    private void setupImageCapture() {
        // Tapping the card shows the chooser dialog
        binding.cardImageCapture.setOnClickListener(v -> showImageChooserDialog());

        // Tapping "Choose from Gallery" goes straight to gallery
        binding.btnChoosePhoto.setOnClickListener(v -> launchGallery());
    }

    /**
     * Presents a {@link MaterialAlertDialogBuilder} dialog letting the user
     * choose between taking a new photo or picking from the gallery.
     */
    private void showImageChooserDialog() {
        String[] options = {
                getString(R.string.btn_take_photo),
                getString(R.string.btn_choose_photo)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_choose_image_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        requestCameraPermissionAndLaunch();
                    } else {
                        launchGallery();
                    }
                })
                .show();
    }

    /**
     * Checks the camera permission and either launches the camera directly
     * or requests permission first.
     */
    private void requestCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Creates a temporary file via {@link FileProvider}, obtains a content URI,
     * and launches the system camera to capture an image into that URI.
     */
    private void launchCamera() {
        File imageDir = new File(getCacheDir(), "images");

        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }

        File imageFile = new File(imageDir, "report_" + System.currentTimeMillis() + ".jpg");

        cameraImageUri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".fileprovider",
                imageFile
        );

        cameraLauncher.launch(cameraImageUri);
    }

    /**
     * Launches the system gallery/file picker filtered to images.
     */
    private void launchGallery() { galleryLauncher.launch("image/*"); }

    /**
     * Loads the given URI into the preview ImageView using Glide and
     * swaps visibility so the placeholder is hidden.
     *
     * @param uri the content URI of the image to display
     */
    private void displaySelectedImage(@NonNull Uri uri) {
        // Hide placeholder, show the preview ImageView
        binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
        binding.imagePreview.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(binding.imagePreview);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  LOCATION PICKER (Embedded Map + Current Location)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Obtains the {@link SupportMapFragment} embedded in the layout and
     * wires up the "Use Current Location" button.
     */
    private void setupLocationPicker() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_picker);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupPlacesAutocomplete();

        binding.btnUseCurrentLocation.setOnClickListener(v -> {
            if (hasLocationPermission()) {
                fetchCurrentLocation();
            } else {
                locationPermissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });
    }

    /**
     * Initializes the Places SDK (reusing the Maps API key from the manifest)
     * and attaches a live address-suggestion dropdown to the location field.
     * Selecting a suggestion resolves the place and moves the map marker.
     * <p>
     * If no API key is configured, autocomplete is silently skipped — manual
     * typing with geocoding at submit time still works.
     * </p>
     */
    private void setupPlacesAutocomplete() {
        String apiKey = readMapsApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return;
        }

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(this);
        placesAdapter = new PlacesAutocompleteAdapter(this, placesClient);
        binding.editLocation.setAdapter(placesAdapter);

        binding.editLocation.setOnItemClickListener((parent, view, position, id) -> {
            AutocompletePrediction prediction = placesAdapter.getPrediction(position);
            if (prediction == null || placesClient == null) {
                return;
            }
            binding.inputLayoutLocation.setError(null);

            FetchPlaceRequest request = FetchPlaceRequest.builder(
                            prediction.getPlaceId(),
                            Arrays.asList(Place.Field.LAT_LNG, Place.Field.ADDRESS))
                    .setSessionToken(placesAdapter.getSessionToken())
                    .build();

            placesClient.fetchPlace(request)
                    .addOnSuccessListener(response -> {
                        placesAdapter.resetSessionToken();
                        LatLng latLng = response.getPlace().getLatLng();
                        if (latLng != null) {
                            placeMarker(latLng);
                            if (googleMap != null) {
                                googleMap.animateCamera(CameraUpdateFactory
                                        .newLatLngZoom(latLng, DEFAULT_ZOOM));
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        placesAdapter.resetSessionToken();
                        // Coordinates unresolved — submit-time geocoding will retry
                    });
        });
    }

    /**
     * Reads the Google Maps API key from the application manifest meta-data.
     */
    @androidx.annotation.Nullable
    private String readMapsApiKey() {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(
                    getPackageName(), PackageManager.GET_META_DATA);
            return info.metaData != null
                    ? info.metaData.getString("com.google.android.geo.API_KEY")
                    : null;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Called when the embedded Google Map is ready.
     * <p>
     * Sets the default camera position, enables zoom controls, and
     * installs a map-click listener that places a marker and reverse-geocodes
     * the tapped position into a readable address.
     * </p>
     */
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(true);

        // Move camera to default location
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));

        // Enable my-location layer if permission is already granted
        if (hasLocationPermission()) {
            try {
                googleMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {
                // Permission was revoked between check and call
            }
        }

        // Handle map taps: place marker + reverse geocode
        googleMap.setOnMapClickListener(latLng -> {
            placeMarker(latLng);
            reverseGeocode(latLng);
        });
    }

    /**
     * Checks whether fine or coarse location permission has been granted.
     *
     * @return {@code true} if at least one location permission is granted
     */
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Uses {@link FusedLocationProviderClient} to fetch the device's
     * current GPS coordinates. On success, places a marker on the map
     * and reverse-geocodes the position into a street address.
     */
    private void fetchCurrentLocation() {
        if (!hasLocationPermission()) {
            return;
        }

        Toast.makeText(this, R.string.msg_fetching_location, Toast.LENGTH_SHORT).show();

        try {
            CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
            fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.getToken()
            ).addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    placeMarker(latLng);
                    reverseGeocode(latLng);

                    if (googleMap != null) {
                        googleMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                    }
                } else {
                    Toast.makeText(this,
                            R.string.msg_location_not_available,
                            Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(this, e -> {
                Toast.makeText(this,
                        R.string.msg_location_not_available,
                        Toast.LENGTH_SHORT).show();
            });
        } catch (SecurityException e) {
            Toast.makeText(this,
                    R.string.msg_location_permission_required,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Places (or moves) a single marker on the map at the given coordinates.
     *
     * @param latLng the position for the marker
     */
    private void placeMarker(@NonNull LatLng latLng) {
        selectedLatLng = latLng;

        if (currentMarker != null) {
            currentMarker.remove();
        }
        if (googleMap != null) {
            currentMarker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(latLng)
                            .title(getString(R.string.hint_item_location))
            );
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    /**
     * Uses the {@link Geocoder} to convert latitude/longitude into a
     * human-readable street address and populates the location EditText.
     *
     * @param latLng the coordinates to reverse-geocode
     */
    private void reverseGeocode(@NonNull LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Build a readable address string from available lines
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(address.getAddressLine(i));
                }
                String addressText = sb.toString();

                binding.editLocation.setText(addressText, false);

                // Update the marker snippet with the address
                if (currentMarker != null) {
                    currentMarker.setSnippet(addressText);
                }
            } else {
                binding.editLocation.setText(
                        String.format(Locale.US, "%.6f, %.6f",
                                latLng.latitude, latLng.longitude), false);
                Toast.makeText(this,
                        R.string.msg_address_not_found,
                        Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            // Geocoder service unavailable — fall back to raw coordinates
            binding.editLocation.setText(
                    String.format(Locale.US, "%.6f, %.6f",
                            latLng.latitude, latLng.longitude), false);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  REPORT TYPE (Lost / Found)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Shows the verification-question card only for Found reports.
     * Lost reports do not collect a verification question.
     */
    private void setupReportTypeChips() {
        binding.chipGroupReportType.setOnCheckedStateChangeListener(
                (group, checkedIds) -> updateVerificationVisibility());
        updateVerificationVisibility();
    }

    private void updateVerificationVisibility() {
        boolean isLost = binding.chipTypeLost.isChecked();
        binding.cardVerification.setVisibility(isLost ? View.GONE : View.VISIBLE);
        if (isLost) {
            binding.inputLayoutVerification.setError(null);
        }
    }

    private boolean isLostReport() {
        return binding.chipTypeLost.isChecked();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  FORM SUBMISSION
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Wires up the submit button.
     */
    private void setupSubmitButton() {
        binding.btnSubmitReport.setOnClickListener(v -> validateAndSubmit());
    }

    /**
     * Validates that all required fields are filled before submitting.
     * Shows appropriate error messages via {@link Toast}.
     */
    private void validateAndSubmit() {
        String title = binding.editItemTitle.getText() != null
                ? binding.editItemTitle.getText().toString().trim() : "";
        String description = binding.editDescription.getText() != null
                ? binding.editDescription.getText().toString().trim() : "";
        String category = binding.dropdownCategory.getText() != null
                ? binding.dropdownCategory.getText().toString().trim() : "";
        String location = binding.editLocation.getText() != null
                ? binding.editLocation.getText().toString().trim() : "";

        if (TextUtils.isEmpty(title)) {
            binding.inputLayoutItemTitle.setError("Please enter a title");
            return;
        } else {
            binding.inputLayoutItemTitle.setError(null);
        }

        if (TextUtils.isEmpty(description)) {
            binding.inputLayoutDescription.setError("Please enter a description");
            return;
        } else {
            binding.inputLayoutDescription.setError(null);
        }

        if (TextUtils.isEmpty(category)) {
            binding.inputLayoutCategory.setError("Please select a category");
            return;
        } else {
            binding.inputLayoutCategory.setError(null);
        }

        if (TextUtils.isEmpty(location)) {
            binding.inputLayoutLocation.setError("Please select a location");
            return;
        } else {
            binding.inputLayoutLocation.setError(null);
        }

        final String verificationQuestion;
        if (!isLostReport()) {
            String question = binding.editVerification.getText() != null
                    ? binding.editVerification.getText().toString().trim() : "";
            if (TextUtils.isEmpty(question)) {
                binding.inputLayoutVerification.setError("Please enter a verification question");
                return;
            }
            binding.inputLayoutVerification.setError(null);
            verificationQuestion = question;
        } else {
            binding.inputLayoutVerification.setError(null);
            verificationQuestion = "";
        }

        String authHeader = TokenManager.getAuthHeader(this);
        if (authHeader == null) {
            Toast.makeText(this, "Please log in to submit a report", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String reportType = binding.chipTypeLost.isChecked() ? "Lost" : "Found";

        if (selectedLatLng != null) {
            // Coordinates already chosen via map tap / current location.
            startUpload(authHeader, reportType, title, description,
                    category, location, verificationQuestion);
            return;
        }

        // No map pin — resolve the typed address into coordinates first.
        setSubmitting(true);
        uploadExecutor.execute(() -> {
            LatLng resolved = forwardGeocode(location);
            runOnUiThread(() -> {
                if (resolved == null) {
                    if (editingItemId == null && !NetworkMonitor.isOnline(this)) {
                        // Geocoder needs network. Queue the report with the
                        // typed address; coordinates stay empty until sync.
                        uploadExecutor.execute(() -> saveReportOffline(
                                reportType, title, description, category,
                                location, verificationQuestion,
                                TokenManager.getUserId(ReportActivity.this)));
                        return;
                    }
                    setSubmitting(false);
                    binding.inputLayoutLocation.setError(
                            getString(R.string.msg_address_not_found_manual));
                    return;
                }
                binding.inputLayoutLocation.setError(null);
                placeMarker(resolved);
                if (googleMap != null) {
                    googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(resolved, DEFAULT_ZOOM));
                }
                startUpload(authHeader, reportType, title, description,
                        category, location, verificationQuestion);
            });
        });
    }

    /**
     * Converts a user-typed address into coordinates using the {@link Geocoder}.
     *
     * @param query the free-form address text
     * @return the resolved coordinates, or {@code null} if the address could
     *         not be resolved
     */
    @androidx.annotation.Nullable
    private LatLng forwardGeocode(@NonNull String query) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> results = geocoder.getFromLocationName(query, 1);
            if (results != null && !results.isEmpty()) {
                Address address = results.get(0);
                return new LatLng(address.getLatitude(), address.getLongitude());
            }
        } catch (IOException ignored) {
            // Geocoder unavailable — treated as unresolved
        }
        return null;
    }

    /**
     * Uploads the image (if any) on a background thread, builds the
     * {@link Item}, and hands it to the backend. Requires
     * {@link #selectedLatLng} to be set.
     */
    private void startUpload(String authHeader, String reportType, String title,
                             String description, String category, String location,
                             String verificationQuestion) {
        setSubmitting(true);

        uploadExecutor.execute(() -> {
            String userId = TokenManager.getUserId(ReportActivity.this);

            // Offline-first: when creating a new report without connectivity,
            // cache it locally and let the background worker sync it later.
            if (editingItemId == null && !NetworkMonitor.isOnline(this)) {
                saveReportOffline(reportType, title, description, category,
                        location, verificationQuestion, userId);
                return;
            }

            String imageUrl = null;
            if (selectedImageUri != null) {
                if (userId == null) {
                    runOnUiThread(() -> {
                        setSubmitting(false);
                        Toast.makeText(this,
                                R.string.msg_report_failed, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                try {
                    imageUrl = ImageUploadService.uploadImage(
                            getApplicationContext(), selectedImageUri, userId);
                } catch (IOException e) {
                    Log.e(TAG, "Firebase image upload failed uri=" + selectedImageUri, e);
                    if (editingItemId == null && !NetworkMonitor.isOnline(this)) {
                        // Connection dropped mid-submit — fall back to the queue.
                        saveReportOffline(reportType, title, description, category,
                                location, verificationQuestion, userId);
                        return;
                    }
                    String detail = e.getMessage() != null
                            ? e.getMessage()
                            : getString(R.string.msg_image_upload_failed);
                    runOnUiThread(() -> {
                        setSubmitting(false);
                        Toast.makeText(this, detail, Toast.LENGTH_LONG).show();
                    });
                    return;
                }
            }

            Item item = buildItem(reportType, title, description, category,
                    location, verificationQuestion,
                    imageUrl != null ? imageUrl : existingImageUrl);

            String finalImageUrl = imageUrl != null ? imageUrl : existingImageUrl;
            runOnUiThread(() -> submitItemToBackend(authHeader, item, finalImageUrl));
        });
    }

    /**
     * Builds the {@link Item} payload from validated form fields. Coordinates
     * are taken from {@link #selectedLatLng} when a pin was placed; queued
     * offline reports may carry none until sync.
     */
    private Item buildItem(String reportType, String title, String description,
                           String category, String location,
                           String verificationQuestion,
                           @androidx.annotation.Nullable String imageUrl) {
        Item item = new Item();
        item.setTitle(title);
        item.setDescription(description);
        item.setCategory(category);
        item.setLocation(location);
        item.setType(reportType);
        item.setVerificationQuestion(verificationQuestion);
        item.setImageUrl(imageUrl);
        if (selectedLatLng != null) {
            item.setLatitude(selectedLatLng.latitude);
            item.setLongitude(selectedLatLng.longitude);
        }
        return item;
    }

    /**
     * Persists the report to the local queue (with a private copy of the
     * selected photo) so the sync worker can push it once connectivity
     * returns. Runs on the upload executor.
     */
    private void saveReportOffline(String reportType, String title,
                                   String description, String category,
                                   String location, String verificationQuestion,
                                   @androidx.annotation.Nullable String userId) {
        Item item = buildItem(reportType, title, description, category,
                location, verificationQuestion, existingImageUrl);
        try {
            PendingReportStore.enqueue(
                    getApplicationContext(), item, selectedImageUri, userId);
            runOnUiThread(() -> {
                setSubmitting(false);
                Toast.makeText(this,
                        R.string.msg_report_saved_offline, Toast.LENGTH_LONG).show();
                finish();
            });
        } catch (IOException e) {
            Log.e(TAG, "Could not save report offline", e);
            runOnUiThread(() -> {
                setSubmitting(false);
                Toast.makeText(this,
                        R.string.msg_report_failed, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void submitItemToBackend(String authHeader, Item item, String imageUrl) {
        Call<Item> call = editingItemId != null
                ? apiService.updateItem(authHeader, editingItemId, item)
                : apiService.createItem(authHeader, item);

        call.enqueue(new Callback<Item>() {
            @Override
            public void onResponse(@NonNull Call<Item> call, @NonNull Response<Item> response) {
                setSubmitting(false);
                if (response.isSuccessful()) {
                    int message = editingItemId != null
                            ? R.string.msg_report_updated
                            : R.string.msg_report_submitted;
                    Toast.makeText(ReportActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ReportActivity.this,
                            R.string.msg_report_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Item> call, @NonNull Throwable t) {
                // Network dropped between validation and POST — queue new
                // reports locally instead of failing.
                if (editingItemId == null && !NetworkMonitor.isOnline(ReportActivity.this)) {
                    Log.w(TAG, "Create failed offline; queueing report locally", t);
                    uploadExecutor.execute(() -> {
                        try {
                            PendingReportStore.enqueue(
                                    getApplicationContext(), item, selectedImageUri,
                                    TokenManager.getUserId(ReportActivity.this));
                            runOnUiThread(() -> {
                                setSubmitting(false);
                                Toast.makeText(ReportActivity.this,
                                        R.string.msg_report_saved_offline,
                                        Toast.LENGTH_LONG).show();
                                finish();
                            });
                        } catch (IOException e) {
                            Log.e(TAG, "Could not queue report after failure", e);
                            runOnUiThread(() -> {
                                setSubmitting(false);
                                Toast.makeText(ReportActivity.this,
                                        R.string.msg_report_failed, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                    return;
                }
                setSubmitting(false);
                Toast.makeText(ReportActivity.this,
                        R.string.msg_report_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setSubmitting(boolean submitting) {
        binding.btnSubmitReport.setEnabled(!submitting);
        binding.btnSubmitReport.setText(submitting
                ? getString(R.string.msg_submitting_report)
                : getString(R.string.btn_submit_report));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uploadExecutor.shutdownNow();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
