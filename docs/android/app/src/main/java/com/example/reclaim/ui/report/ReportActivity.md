```java
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
    targetLostReportId = getIntent().getStringExtra(EXTRA_TARGET_LOST_REPORT_ID);
    lockFoundType = getIntent().getBooleanExtra(EXTRA_LOCK_FOUND_TYPE, false)
            || !TextUtils.isEmpty(targetLostReportId);
    applyFoundTypeLock();

    if (editingItemId != null) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.report_edit_title);
        }

        loadExistingItem(editingItemId);
    }
}
```

What that function does: I show the report form and I load an existing item when editing.
What the function is responsible for: I am the report start.
Where it works: DashboardActivity FAB, MyItemsFragment edit, ItemDetailsActivity I-found-this, and OfflineProtocol open me. I inflate activity_report.xml.
Why I chose to write the code of the function the way its written: Lock found type if this form is tied to a lost item. Edit title only when EXTRA_ITEM_ID is set.
More: I use ActivityReportBinding and FusedLocationProviderClient.

```java
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
```

What that function does: I fetch the item I am editing.
What the function is responsible for: I am the edit load.
Where it works: onCreate calls me when EXTRA_ITEM_ID is set.
Why I chose to write the code of the function the way its written: No token means I skip rather than crash. A miss shows a generic toast.
More: I use getItemById.

```java
public void onResponse(@NonNull Call<Item> call, @NonNull Response<Item> response) {
    if (!response.isSuccessful() || response.body() == null) {
        Toast.makeText(ReportActivity.this,
                R.string.error_generic, Toast.LENGTH_SHORT).show();
        return;
    }
    populateForm(response.body());
}
```

What that function does: I fill the form from the server item.
What the function is responsible for: I am the success path for the edit fetch.
Where it works: Retrofit calls me from loadExistingItem.
Why I chose to write the code of the function the way its written: A bad body gets the same toast as a miss.
More: I call populateForm.

```java
public void onFailure(@NonNull Call<Item> call, @NonNull Throwable t) {
    Toast.makeText(ReportActivity.this,
            R.string.error_generic, Toast.LENGTH_SHORT).show();
}
```

What that function does: I toast if the edit fetch fails.
What the function is responsible for: I am the failure path for the edit fetch.
Where it works: Retrofit calls me from loadExistingItem.
Why I chose to write the code of the function the way its written: The empty form is still usable. A short toast is enough.
More: I use error_generic.

```java
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
        displayImage(existingImageUrl);
    }

    if (item.getLatitude() != null && item.getLongitude() != null) {
        selectedLatLng = new LatLng(item.getLatitude(), item.getLongitude());

        if (googleMap != null) {
            placeMarker(selectedLatLng);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, DEFAULT_ZOOM));
        }
    }
}
```

What that function does: I copy an existing report into the form, photo, and map pin.
What the function is responsible for: I am the edit bind.
Where it works: loadExistingItem's onResponse calls me.
Why I chose to write the code of the function the way its written: setText(..., false) avoids firing autocomplete. The pin waits if the map is not ready yet.
More: I keep existingImageUrl so a later save can reuse the old photo.

```java
private void setupToolbar() {
    setSupportActionBar(binding.toolbarReport);

    if (getSupportActionBar() != null) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }
}
```

What that function does: I add a back arrow on the report toolbar.
What the function is responsible for: I am the report toolbar.
Where it works: onCreate calls me. Back goes through onSupportNavigateUp.
Why I chose to write the code of the function the way its written: XML already has the title. I only turn on up.
More: I use setSupportActionBar.

```java
private void setupCategoryDropdown() {
    ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            R.layout.item_dropdown_category,
            getResources().getStringArray(R.array.item_categories)
    );
    binding.dropdownCategory.setAdapter(adapter);
}
```

What that function does: I fill the category list from string-array.
What the function is responsible for: I am the category dropdown.
Where it works: onCreate calls me. The dropdown uses item_dropdown_category.xml.
Why I chose to write the code of the function the way its written: Categories live in resources so I do not hardcode them.
More: I use ArrayAdapter.

```java
private void setupImageCapture() {
    // Card tap opens the chooser
    binding.cardImageCapture.setOnClickListener(v -> showImageChooserDialog());

    // Photo tap also opens the chooser, so it can be replaced.
    binding.imagePreview.setOnClickListener(v -> showImageChooserDialog());

    // Gallery button skips the chooser
    binding.btnChoosePhoto.setOnClickListener(v -> launchGallery());
}
```

What that function does: I wire photo taps to camera/gallery.
What the function is responsible for: I am photo click setup.
Where it works: onCreate calls me.
Why I chose to write the code of the function the way its written: Choose photo skips the dialog because the label already says gallery.
More: I call showImageChooserDialog and launchGallery.

```java
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
```

What that function does: I ask camera or gallery.
What the function is responsible for: I am the photo chooser.
Where it works: cardImageCapture and imagePreview call me.
Why I chose to write the code of the function the way its written: Two items is enough. Camera may still need permission.
More: I use MaterialAlertDialogBuilder.setItems.

```java
private void requestCameraPermissionAndLaunch() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
        launchCamera();
    } else {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }
}
```

What that function does: I open the camera if allowed, or I ask first.
What the function is responsible for: I gate the camera.
Where it works: The chooser camera item calls me.
Why I chose to write the code of the function the way its written: I do not launch without CAMERA.
More: I use cameraPermissionLauncher.

```java
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
```

What that function does: I make a cache jpeg and open the camera into it.
What the function is responsible for: I am TakePicture.
Where it works: requestCameraPermissionAndLaunch and the permission launcher call me.
Why I chose to write the code of the function the way its written: FileProvider shares the file with the camera app. Cache keeps it off the gallery.
More: I use ActivityResultContracts.TakePicture.

```java
private void launchGallery() { galleryLauncher.launch("image/*"); }
```

What that function does: I open the gallery for images only.
What the function is responsible for: I am GetContent.
Where it works: setupImageCapture and the chooser gallery item call me.
Why I chose to write the code of the function the way its written: image/* keeps videos out.
More: I use galleryLauncher.

```java
private void displaySelectedImage(@NonNull Uri uri) {
    displayImage(uri);
}
```

What that function does: I show a local photo the user just picked.
What the function is responsible for: I am the camera/gallery preview path.
Where it works: cameraLauncher and galleryLauncher call me.
Why I chose to write the code of the function the way its written: Display is shared with URL preview through displayImage.
More: I pass the Uri through.

```java
private void displayImage(@NonNull Object model) {
    // Hide the placeholder
    binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
    binding.imagePreview.setVisibility(View.VISIBLE);

    Glide.with(this)
            .load(model)
            .centerCrop()
            .into(binding.imagePreview);
}
```

What that function does: I hide the camera placeholder and show a photo.
What the function is responsible for: I am the preview bind.
Where it works: displaySelectedImage and populateForm call me.
Why I chose to write the code of the function the way its written: Object lets Glide take a Uri or a URL string.
More: I use Glide.centerCrop.

```java
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
```

What that function does: I start the map, Places typing, and the current-place button.
What the function is responsible for: I am location setup.
Where it works: onCreate calls me. MapTouchWrapper wraps map_picker in activity_report.xml.
Why I chose to write the code of the function the way its written: Fine or coarse is enough to fetch. Places can no-op if the key is missing.
More: I use SupportMapFragment.getMapAsync.

```java
private void setupPlacesAutocomplete() {
    String apiKey = readMapsApiKey();
    if (apiKey == null || apiKey.isEmpty()) {
        Log.e(TAG, "Places key is missing. Autocomplete will not run.");
        return;
    }

    try {
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
            Log.d(TAG, "Places.initialize() completed before autocomplete requests.");
        }
    } catch (Exception e) {
        Log.e(TAG, "Places.initialize() failed", e);
        return;
    }
    placesClient = Places.createClient(this);
    placesAdapter = new PlacesAutocompleteAdapter(this, placesClient);
    placesAdapter.bindField(binding.editLocation);
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
                    // Place not found yet; submit will try again
                });
    });
}
```

What that function does: I add address suggestions and drop a pin when one is picked.
What the function is responsible for: I am Places autocomplete.
Where it works: setupLocationPicker calls me. PlacesAutocompleteAdapter uses item_places_dropdown.xml.
Why I chose to write the code of the function the way its written: No key means typing still works. Submit can geocode later. Session token is reset after fetch.
More: I use PlacesClient.fetchPlace.

```java
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
```

What that function does: I read the Maps key from the app manifest.
What the function is responsible for: I supply Places.initialize.
Where it works: setupPlacesAutocomplete calls me.
Why I chose to write the code of the function the way its written: The key already lives in meta-data for the map SDK. I reuse it.
More: I use GET_META_DATA.

```java
@Override
public void onMapReady(@NonNull GoogleMap map) {
    this.googleMap = map;

    googleMap.getUiSettings().setZoomControlsEnabled(true);
    googleMap.getUiSettings().setScrollGesturesEnabled(true);

    // Start at the default place
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));

    // Show the blue location dot if allowed
    if (hasLocationPermission()) {
        try {
            googleMap.setMyLocationEnabled(true);
        } catch (SecurityException ignored) {
            // Permission was taken away
        }
    }

    // Tap to drop a pin and fill the address
    googleMap.setOnMapClickListener(latLng -> {
        placeMarker(latLng);
        reverseGeocode(latLng);
    });
}
```

What that function does: I keep the map, start at Tel Aviv, and drop a pin on tap.
What the function is responsible for: I am OnMapReadyCallback.
Where it works: SupportMapFragment.getMapAsync calls me.
Why I chose to write the code of the function the way its written: populateForm may have set selectedLatLng before the map was ready; a tap still works. Edit pin is placed in populateForm if the map exists.
More: I use setOnMapClickListener.

```java
private boolean hasLocationPermission() {
    return ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
}
```

What that function does: I check fine or coarse location.
What the function is responsible for: I gate fetch and my-location.
Where it works: setupLocationPicker, onMapReady, and fetchCurrentLocation call me.
Why I chose to write the code of the function the way its written: Coarse is enough to drop a rough pin.
More: I use ContextCompat.checkSelfPermission.

```java
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
```

What that function does: I get a fresh location, pin it, and fill the address.
What the function is responsible for: I am use-current-location.
Where it works: The current-location button and the permission launcher call me.
Why I chose to write the code of the function the way its written: getCurrentLocation is fresher than last location for a report pin.
More: I use Priority.PRIORITY_HIGH_ACCURACY.

```java
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
```

What that function does: I keep one pin and remember its LatLng.
What the function is responsible for: I am the map pin.
Where it works: Map tap, current location, Places pick, geocode, and populateForm call me.
Why I chose to write the code of the function the way its written: Removing the old pin avoids a stack of markers.
More: I store selectedLatLng even if the map is still null.

```java
private void reverseGeocode(@NonNull LatLng latLng) {
    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
    try {
        List<Address> addresses = geocoder.getFromLocation(
                latLng.latitude, latLng.longitude, 1);

        if (addresses != null && !addresses.isEmpty()) {
            Address address = addresses.get(0);

            // Join the address lines
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(address.getAddressLine(i));
            }
            String addressText = sb.toString();

            binding.editLocation.setText(addressText, false);

            // Put the address on the pin
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
        // No address; show the numbers instead
        binding.editLocation.setText(
                String.format(Locale.US, "%.6f, %.6f",
                        latLng.latitude, latLng.longitude), false);
    }
}
```

What that function does: I turn a pin into a street address, or I show the numbers.
What the function is responsible for: I fill edit_location after a map tap.
Where it works: onMapReady's click and fetchCurrentLocation call me.
Why I chose to write the code of the function the way its written: Numbers still let submit go through if Geocoder is empty.
More: I use Geocoder.getFromLocation.

```java
private void setupReportTypeChips() {
    binding.chipGroupReportType.setOnCheckedStateChangeListener(
            (group, checkedIds) -> {
                if (lockFoundType && !binding.chipTypeFound.isChecked()) {
                    binding.chipTypeFound.setChecked(true);
                }
                updateVerificationVisibility();
            });
    updateVerificationVisibility();
}
```

What that function does: I keep found locked if needed, and I show the check question only for found.
What the function is responsible for: I am lost/found chips.
Where it works: onCreate calls me.
Why I chose to write the code of the function the way its written: Re-checking found inside the listener stops I-found-this from flipping to lost.
More: I use ChipGroup.

```java
private void applyFoundTypeLock() {
    if (!lockFoundType) {
        return;
    }
    binding.chipTypeFound.setChecked(true);
    binding.chipTypeLost.setEnabled(false);
    if (getSupportActionBar() != null) {
        getSupportActionBar().setTitle(R.string.report_found_for_lost_title);
    }
    updateVerificationVisibility();
}
```

What that function does: I force a found report when this form is for someone else's lost item.
What the function is responsible for: I am the I-found-this lock.
Where it works: onCreate calls me after reading EXTRA_LOCK_FOUND_TYPE / EXTRA_TARGET_LOST_REPORT_ID.
Why I chose to write the code of the function the way its written: Disabling lost is clearer than hiding the chip.
More: I change the toolbar title.

```java
private void updateVerificationVisibility() {
    boolean isLost = binding.chipTypeLost.isChecked();
    binding.cardVerification.setVisibility(isLost ? View.GONE : View.VISIBLE);
    if (isLost) {
        binding.inputLayoutVerification.setError(null);
    }
}
```

What that function does: I hide the check question on lost reports.
What the function is responsible for: I am the verification card show/hide.
Where it works: Chip changes, found lock, and populateForm call me.
Why I chose to write the code of the function the way its written: Claim answers belong on found items. Lost reports do not need a question.
More: I clear the error when hiding.

```java
private boolean isLostReport() {
    return binding.chipTypeLost.isChecked();
}
```

What that function does: I tell whether lost is selected.
What the function is responsible for: I skip the question on lost submit.
Where it works: validateAndSubmit calls me.
Why I chose to write the code of the function the way its written: One chip read is enough.
More: I use chipTypeLost.

```java
private void setupSubmitButton() {
    binding.btnSubmitReport.setOnClickListener(v -> validateAndSubmit());
}
```

What that function does: I wire submit to validation.
What the function is responsible for: I am the submit click.
Where it works: onCreate calls me.
Why I chose to write the code of the function the way its written: Validation lives in one method.
More: I call validateAndSubmit.

```java
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
        binding.inputLayoutItemTitle.setError(getString(R.string.msg_enter_title));
        return;
    } else {
        binding.inputLayoutItemTitle.setError(null);
    }

    if (TextUtils.isEmpty(description)) {
        binding.inputLayoutDescription.setError(getString(R.string.msg_enter_description));
        return;
    } else {
        binding.inputLayoutDescription.setError(null);
    }

    if (TextUtils.isEmpty(category)) {
        binding.inputLayoutCategory.setError(getString(R.string.msg_select_category));
        return;
    } else {
        binding.inputLayoutCategory.setError(null);
    }

    if (TextUtils.isEmpty(location)) {
        binding.inputLayoutLocation.setError(getString(R.string.msg_select_location));
        return;
    } else {
        binding.inputLayoutLocation.setError(null);
    }

    final String verificationQuestion;
    if (!isLostReport()) {
        String question = binding.editVerification.getText() != null
                ? binding.editVerification.getText().toString().trim() : "";
        if (TextUtils.isEmpty(question)) {
            binding.inputLayoutVerification.setError(getString(R.string.msg_enter_verification_question));
            return;
        }
        binding.inputLayoutVerification.setError(null);
        verificationQuestion = question;
    } else {
        binding.inputLayoutVerification.setError(null);
        verificationQuestion = "";
    }

    String authHeader = TokenManager.getAuthHeader(this);
    if (editingItemId != null && authHeader == null) {
        Toast.makeText(this, R.string.msg_login_to_submit_report, Toast.LENGTH_SHORT).show();
        finish();
        return;
    }

    String reportType = binding.chipTypeLost.isChecked() ? "Lost" : "Found";

    if (authHeader == null) {
        queueGuestReport(reportType, title, description, category,
                location, verificationQuestion);
        return;
    }

    if (selectedLatLng != null) {
        // Pin already set.
        startUpload(authHeader, reportType, title, description,
                category, location, verificationQuestion);
        return;
    }

    // No pin yet. Turn the typed address into a place first.
    setSubmitting(true);
    uploadExecutor.execute(() -> {
        LatLng resolved = forwardGeocode(location);
        runOnUiThread(() -> {
            if (resolved == null) {
                if (editingItemId == null && !NetworkMonitor.isBackendAvailable(this)) {
                    // No network for the address. Save the typed text; place can wait.
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
```

What that function does: I check the form, then I upload, geocode, queue offline, or save as a guest.
What the function is responsible for: I am submit.
Where it works: btnSubmitReport calls me.
Why I chose to write the code of the function the way its written: Field errors stay on the layouts. Edit needs a token. Guest and offline still keep the typed address.
More: I use NetworkMonitor.isBackendAvailable and forwardGeocode.

```java
private void queueGuestReport(String reportType, String title,
                              String description, String category,
                              String location, String verificationQuestion) {
    setSubmitting(true);
    uploadExecutor.execute(() -> {
        if (selectedLatLng == null) {
            LatLng resolved = forwardGeocode(location);
            if (resolved != null) {
                selectedLatLng = resolved;
            }
        }
        saveReportOffline(reportType, title, description, category,
                location, verificationQuestion, null);
    });
}
```

What that function does: I save a report with no owner until the user signs in.
What the function is responsible for: I am guest submit.
Where it works: validateAndSubmit calls me when there is no auth header.
Why I chose to write the code of the function the way its written: Geocode on the worker if I can. A missing pin still queues the typed place.
More: I pass null userId into saveReportOffline.

```java
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
        // Could not find the place
    }
    return null;
}
```

What that function does: I turn a typed address into a LatLng.
What the function is responsible for: I fill a missing pin before upload.
Where it works: validateAndSubmit and queueGuestReport call me on uploadExecutor.
Why I chose to write the code of the function the way its written: Geocoder can block, so I keep it off the UI thread.
More: I use getFromLocationName.

```java
private void startUpload(String authHeader, String reportType, String title,
                         String description, String category, String location,
                         String verificationQuestion) {
    setSubmitting(true);

    uploadExecutor.execute(() -> {
        String userId = TokenManager.getUserId(ReportActivity.this);

        // No connection. Save locally and send later.
        if (editingItemId == null && !NetworkMonitor.isBackendAvailable(this)) {
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
                if (editingItemId == null && !NetworkMonitor.isBackendAvailable(this)) {
                    // Connection dropped. Save locally instead.
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
```

What that function does: I upload a new photo if needed, then I POST or PUT the report. Offline new reports queue locally.
What the function is responsible for: I am the signed-in save path.
Where it works: validateAndSubmit calls me after a pin exists.
Why I chose to write the code of the function the way its written: Photo upload can block, so I use uploadExecutor. Edit does not fall back to the offline queue.
More: I use ImageUploadService.uploadImage and NetworkMonitor.

```java
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
    if (!TextUtils.isEmpty(targetLostReportId)) {
        item.setTargetLostReportId(targetLostReportId);
    }
    return item;
}
```

What that function does: I copy the form into an Item. Offline reports may have no pin yet.
What the function is responsible for: I am the report body.
Where it works: startUpload and saveReportOffline call me.
Why I chose to write the code of the function the way its written: targetLostReportId ties a found report to the lost item from details.
More: I use Item setters.

```java
private void saveReportOffline(String reportType, String title,
                               String description, String category,
                               String location, String verificationQuestion,
                               @androidx.annotation.Nullable String userId) {
    Item item = buildItem(reportType, title, description, category,
            location, verificationQuestion, existingImageUrl);
    try {
        PendingReportStore.enqueue(
                getApplicationContext(), item, selectedImageUri, userId);
        runOnUiThread(this::finishAfterOfflineSave);
    } catch (IOException e) {
        Log.e(TAG, "Could not save report offline", e);
        runOnUiThread(() -> {
            setSubmitting(false);
            Toast.makeText(this,
                    R.string.msg_report_failed, Toast.LENGTH_SHORT).show();
        });
    }
}
```

What that function does: I copy the report and photo into local storage to send later.
What the function is responsible for: I am the offline queue.
Where it works: startUpload, queueGuestReport, geocode miss, and submit failures call me.
Why I chose to write the code of the function the way its written: PendingReportStore owns the Room row. I finish on the UI thread after enqueue.
More: I use PendingReportStore.enqueue.

```java
private void finishAfterOfflineSave() {
    setSubmitting(false);
    notifyTargetedFoundSubmitted();
    Toast.makeText(getApplicationContext(),
            R.string.msg_report_saved, Toast.LENGTH_LONG).show();
    finishAffinity();
}
```

What that function does: I toast that it was saved, mark a targeted found if needed, then I close the task.
What the function is responsible for: I leave after an offline save.
Where it works: saveReportOffline and submit failure queue paths call me.
Why I chose to write the code of the function the way its written: finishAffinity matches OfflineProtocol's leave path so the user is not stuck on a dead form.
More: I use getApplicationContext for the toast in case the activity is finishing.

```java
private void notifyTargetedFoundSubmitted() {
    if (editingItemId == null && !TextUtils.isEmpty(targetLostReportId)) {
        AppStateEvents.markItemInteracted(targetLostReportId);
        setResult(RESULT_OK);
    }
}
```

What that function does: I mark the lost item as already acted on and return OK.
What the function is responsible for: I tell details the found report was filed.
Where it works: finishAfterOfflineSave and a successful create call me.
Why I chose to write the code of the function the way its written: ItemDetailsActivity's foundReportLauncher listens for RESULT_OK.
More: I use AppStateEvents.markItemInteracted.

```java
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
                notifyTargetedFoundSubmitted();
                finish();
            } else {
                if (editingItemId == null
                        && !NetworkMonitor.isBackendAvailable(ReportActivity.this)) {
                    uploadExecutor.execute(() -> {
                        try {
                            PendingReportStore.enqueue(
                                    getApplicationContext(), item, selectedImageUri,
                                    TokenManager.getUserId(ReportActivity.this));
                            runOnUiThread(ReportActivity.this::finishAfterOfflineSave);
                        } catch (IOException e) {
                            Log.e(TAG, "Could not queue report after gateway failure", e);
                            runOnUiThread(() -> {
                                setSubmitting(false);
                                ConnectionToasts.showIfOnline(ReportActivity.this,
                                        R.string.msg_report_failed, Toast.LENGTH_SHORT);
                            });
                        }
                    });
                    return;
                }
                ConnectionToasts.showIfOnline(ReportActivity.this,
                        R.string.msg_report_failed, Toast.LENGTH_SHORT);
            }
        }

        @Override
        public void onFailure(@NonNull Call<Item> call, @NonNull Throwable t) {
            // Connection dropped. Save new reports locally.
            if (editingItemId == null && !NetworkMonitor.isBackendAvailable(ReportActivity.this)) {
                Log.w(TAG, "Create failed offline; queueing report locally", t);
                uploadExecutor.execute(() -> {
                    try {
                        PendingReportStore.enqueue(
                                getApplicationContext(), item, selectedImageUri,
                                TokenManager.getUserId(ReportActivity.this));
                        runOnUiThread(ReportActivity.this::finishAfterOfflineSave);
                    } catch (IOException e) {
                        Log.e(TAG, "Could not queue report after failure", e);
                        runOnUiThread(() -> {
                            setSubmitting(false);
                            ConnectionToasts.showIfOnline(ReportActivity.this,
                                    R.string.msg_report_failed, Toast.LENGTH_SHORT);
                        });
                    }
                });
                return;
            }
            setSubmitting(false);
            ConnectionToasts.showIfOnline(ReportActivity.this,
                    R.string.msg_report_failed, Toast.LENGTH_SHORT);
        }
    });
}
```

What that function does: I create or update the report. A new report that hits no backend is queued locally.
What the function is responsible for: I am the Retrofit save.
Where it works: startUpload posts me on the UI thread.
Why I chose to write the code of the function the way its written: Edit stays online-only. Create can fall back to PendingReportStore.
More: I use createItem and updateItem. imageUrl is unused after buildItem already set it.

```java
public void onResponse(@NonNull Call<Item> call, @NonNull Response<Item> response) {
    setSubmitting(false);
    if (response.isSuccessful()) {
        int message = editingItemId != null
                ? R.string.msg_report_updated
                : R.string.msg_report_submitted;
        Toast.makeText(ReportActivity.this, message, Toast.LENGTH_SHORT).show();
        notifyTargetedFoundSubmitted();
        finish();
    } else {
        if (editingItemId == null
                && !NetworkMonitor.isBackendAvailable(ReportActivity.this)) {
            uploadExecutor.execute(() -> {
                try {
                    PendingReportStore.enqueue(
                            getApplicationContext(), item, selectedImageUri,
                            TokenManager.getUserId(ReportActivity.this));
                    runOnUiThread(ReportActivity.this::finishAfterOfflineSave);
                } catch (IOException e) {
                    Log.e(TAG, "Could not queue report after gateway failure", e);
                    runOnUiThread(() -> {
                        setSubmitting(false);
                        ConnectionToasts.showIfOnline(ReportActivity.this,
                                R.string.msg_report_failed, Toast.LENGTH_SHORT);
                    });
                }
            });
            return;
        }
        ConnectionToasts.showIfOnline(ReportActivity.this,
                R.string.msg_report_failed, Toast.LENGTH_SHORT);
    }
}
```

What that function does: I finish on success, or I queue a new report if the gateway is down.
What the function is responsible for: I am the create/update success path.
Where it works: Retrofit calls me from submitItemToBackend.
Why I chose to write the code of the function the way its written: A down gateway should not lose a new report the user already filled.
More: I use PendingReportStore.enqueue.

```java
public void onFailure(@NonNull Call<Item> call, @NonNull Throwable t) {
    // Connection dropped. Save new reports locally.
    if (editingItemId == null && !NetworkMonitor.isBackendAvailable(ReportActivity.this)) {
        Log.w(TAG, "Create failed offline; queueing report locally", t);
        uploadExecutor.execute(() -> {
            try {
                PendingReportStore.enqueue(
                        getApplicationContext(), item, selectedImageUri,
                        TokenManager.getUserId(ReportActivity.this));
                runOnUiThread(ReportActivity.this::finishAfterOfflineSave);
            } catch (IOException e) {
                Log.e(TAG, "Could not queue report after failure", e);
                runOnUiThread(() -> {
                    setSubmitting(false);
                    ConnectionToasts.showIfOnline(ReportActivity.this,
                            R.string.msg_report_failed, Toast.LENGTH_SHORT);
                });
            }
        });
        return;
    }
    setSubmitting(false);
    ConnectionToasts.showIfOnline(ReportActivity.this,
            R.string.msg_report_failed, Toast.LENGTH_SHORT);
}
```

What that function does: I queue a new report if create failed offline, else I toast.
What the function is responsible for: I am the create/update failure path.
Where it works: Retrofit calls me from submitItemToBackend.
Why I chose to write the code of the function the way its written: Same offline fallback as a bad gateway status.
More: I use NetworkMonitor.isBackendAvailable.

```java
private void setSubmitting(boolean submitting) {
    binding.btnSubmitReport.setEnabled(!submitting);
    binding.btnSubmitReport.setText(submitting
            ? getString(R.string.msg_submitting_report)
            : getString(R.string.btn_submit_report));
}
```

What that function does: I lock the submit button and show Submitting.
What the function is responsible for: I stop a second tap.
Where it works: validateAndSubmit, upload, and finish paths call me.
Why I chose to write the code of the function the way its written: Changing the label is clearer than a separate spinner on this long form.
More: I set enabled and text together.

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    uploadExecutor.shutdownNow();
}
```

What that function does: I stop the upload thread when the screen dies.
What the function is responsible for: I am cleanup.
Where it works: Android calls me.
Why I chose to write the code of the function the way its written: shutdownNow drops a stuck geocode or photo upload.
More: I use ExecutorService.shutdownNow.

```java
@Override
public boolean onSupportNavigateUp() {
    getOnBackPressedDispatcher().onBackPressed();
    return true;
}
```

What that function does: I treat the toolbar back arrow as system back.
What the function is responsible for: I leave the form.
Where it works: The toolbar up button calls me.
Why I chose to write the code of the function the way its written: Same path as the system back key.
More: I return true so the event is consumed.
