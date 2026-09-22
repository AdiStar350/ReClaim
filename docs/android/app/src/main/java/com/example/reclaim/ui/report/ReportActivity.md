# ReportActivity.java

## Overview
This is the create/edit report **Activity** (see `docs/GLOSSARY.md`). The form collects title, description, category, lost/found type, optional verification question (found only), photo (camera or gallery), and a map/Places location. It can create or update an item online, queue offline for later sync, or create a “found for this lost item” report when launched with lock extras.

## Concepts used here
- **Activity** — full report form screen. See `docs/GLOSSARY.md`.
- **Google Maps / Places** — map pin, autocomplete, fetch place lat/lng. See `docs/GLOSSARY.md`.
- **Geocoder** — reverse (pin → address) and forward (typed address → lat/lng).
- **Firebase Storage (ImageUploadService)** — uploads the photo before create/update. See `docs/GLOSSARY.md`.
- **Retrofit Call / Callback** — create/update item, load existing for edit. See `docs/GLOSSARY.md`.
- **WorkManager / offline** — `PendingReportStore` + `NetworkMonitor` when the backend is unreachable. See `docs/GLOSSARY.md`.
- **Activity Result API** — camera, gallery, camera/location permissions.
- **FileProvider** — shares a cache file Uri with the camera app.

## Libraries and imports
- **AndroidX / AppCompat**: AppCompatActivity, FileProvider, ContextCompat, Activity Result contracts.
- **Google Maps / Places / Location**: SupportMapFragment, GoogleMap, Marker, PlacesClient, FusedLocationProviderClient, Priority.
- **Glide**: photo preview.
- **Material**: dialogs, chips, text fields (via binding).
- **Project**: `PlacesAutocompleteAdapter`, `Item`, `TokenManager`, `ImageUploadService`, `PendingReportStore`, `NetworkMonitor`, `AppStateEvents`, `ConnectionToasts`.

## Fields
- Public extras: `EXTRA_ITEM_ID` (edit), `EXTRA_LOCK_FOUND_TYPE`, `EXTRA_TARGET_LOST_REPORT_ID` (found-for-lost).
- `DEFAULT_ZOOM` / `DEFAULT_LOCATION` — map defaults (Tel Aviv).
- `binding`, `apiService`, `uploadExecutor` — UI, API, background upload/geocode/offline save.
- Photo: `cameraImageUri`, `selectedImageUri`; edit: `existingImageUrl`, `editingItemId`.
- Map/place: `googleMap`, `currentMarker`, `fusedLocationClient`, `selectedLatLng`, `placesAdapter`, `placesClient`.
- Mode: `targetLostReportId`, `lockFoundType`.
- Launchers: camera, gallery, camera permission, location permissions.

## Functions

### `private void setupToolbar()` / `setupCategoryDropdown()` / `setupSubmitButton()`
```java
private void setupToolbar() { /* action bar + home as up */ }
private void setupCategoryDropdown() { /* ArrayAdapter from R.array.item_categories */ }
private void setupSubmitButton() { binding.btnSubmitReport.setOnClickListener(v -> validateAndSubmit()); }
```
- **What it does:** Basic chrome, category list, submit wiring.
- **Where it's called:** From `onCreate`.
- **Why it's written this way:** Categories come from resources so the list stays in one place.
- **Step by step:**
  1. Toolbar with up navigation.
  2. Category dropdown adapter from string array.
  3. Submit → `validateAndSubmit`.

### `private void setupImageCapture()` / `showImageChooserDialog` / `requestCameraPermissionAndLaunch` / `launchCamera` / `launchGallery` / `displaySelectedImage` / `displayImage`
```java
private void setupImageCapture() { /* card/preview → chooser; btn → gallery */ }
private void showImageChooserDialog() { /* take photo vs choose photo */ }
private void launchCamera() { /* cache file + FileProvider + TakePicture */ }
private void displayImage(@NonNull Object model) { /* hide placeholder; Glide */ }
```
- **What it does:** Lets the user pick camera or gallery, shows a preview, and stores `selectedImageUri`.
- **Where it's called:** Setup from `onCreate`; chooser from card/preview clicks; camera/gallery from chooser or gallery button; display from launchers and `populateForm`.
- **Why it's written this way:** Camera needs a FileProvider Uri written ahead of time. Gallery uses `image/*`. `displayImage` accepts Uri or URL string.
- **Step by step:**
  1. Chooser routes to permission+camera or gallery.
  2. Camera creates `report_<time>.jpg` under cache `images/`.
  3. On success, set `selectedImageUri` and Glide-preview.

### `private String readMapsApiKey()` / `setupPlacesAutocomplete()` / `setupLocationPicker()`
```java
private String readMapsApiKey() { /* meta-data com.google.android.geo.API_KEY */ }

private void setupPlacesAutocomplete() {
    // Places.initialize; PlacesAutocompleteAdapter; on pick → fetchPlace → marker
}

private void setupLocationPicker() {
    // map async; places autocomplete; use-current-location button
}
```
- **What it does:** Wires the map fragment, “use current location,” and Places autocomplete on the location field.
- **Where it's called:** From `onCreate` via `setupLocationPicker`.
- **Why it's written this way:** Without an API key, autocomplete is skipped but typing still works; forward geocode on submit can still resolve. Session token resets after fetch success/failure.
- **Step by step:**
  1. Read key from manifest meta-data; init Places; bind adapter.
  2. On suggestion click: `fetchPlace` for LAT_LNG/ADDRESS; place marker; animate camera.
  3. Current-location button requests permissions or `fetchCurrentLocation`.

### `public void onMapReady(GoogleMap map)` / `hasLocationPermission` / `fetchCurrentLocation` / `placeMarker` / `reverseGeocode`
```java
@Override
public void onMapReady(@NonNull GoogleMap map) {
    // default camera; my-location if allowed; tap → placeMarker + reverseGeocode
}

private void placeMarker(@NonNull LatLng latLng) { /* one marker; store selectedLatLng */ }

private void reverseGeocode(@NonNull LatLng latLng) {
    // Geocoder → fill editLocation or lat,lng text
}
```
- **What it does:** Configures the map, drops a single pin, fills the address field from the pin (or coordinates if geocode fails).
- **Where it's called:** Map ready callback; tap listener; current-location success; Places pick; form populate; submit after forward geocode.
- **Why it's written this way:** One marker at a time. Reverse geocode runs on the UI thread (simple Geocoder call); failure falls back to numeric coordinates.
- **Step by step:**
  1. Enable zoom/scroll; start at DEFAULT_LOCATION.
  2. On tap or location: update `selectedLatLng`, replace marker, reverse-geocode into the field.

### `private void setupReportTypeChips()` / `applyFoundTypeLock()` / `updateVerificationVisibility()` / `isLostReport()`
```java
private void applyFoundTypeLock() {
    // force Found chip; disable Lost; special title
}

private void updateVerificationVisibility() {
    // verification card only for Found
}
```
- **What it does:** Lost/Found chips; hides verification for Lost; locks Found when opened from “I found this.”
- **Where it's called:** Setup from `onCreate`; chip changes; lock after reading Intent extras.
- **Why it's written this way:** Found reports need a verification question for later claims; locked mode prevents switching back to Lost.
- **Step by step:**
  1. Chip listener re-checks Found if locked and updates verification visibility.
  2. Lock checks Found, disables Lost, sets title.

### `private void loadExistingItem(String itemId)` / `populateForm(Item item)`
```java
private void loadExistingItem(String itemId) {
    apiService.getItemById(...).enqueue(/* populateForm */);
}

private void populateForm(@NonNull Item item) {
    // fill fields, type chip, image, lat/lng marker if present
}
```
- **What it does:** Loads an existing report for edit mode and fills the form.
- **Where it's called:** `onCreate` when `EXTRA_ITEM_ID` is set (from `MyItemsFragment.onEdit`).
- **Why it's written this way:** Keeps edit and create on one screen. Existing image URL is kept if the user does not pick a new photo.
- **Step by step:**
  1. Fetch by id; on success `populateForm`.
  2. Set texts/chips; `displayImage` if URL; place marker if coordinates exist.

### `private LatLng forwardGeocode(String query)` / `private Item buildItem(...)`
```java
private LatLng forwardGeocode(@NonNull String query) {
    // Geocoder.getFromLocationName → LatLng or null
}

private Item buildItem(...) {
    // set fields; lat/lng if selectedLatLng; targetLostReportId if set
}
```
- **What it does:** Resolves a typed address to coordinates; builds the `Item` payload for create/update/offline.
- **Where it's called:** Forward geocode from submit paths and guest queue; `buildItem` from upload/offline save.
- **Why it's written this way:** Offline saves may omit coordinates if geocode failed; targeted found sets `targetLostReportId`.
- **Step by step:**
  1. Forward: first Geocoder result → LatLng, else null.
  2. Build: copy form fields onto a new `Item`.

### `private void validateAndSubmit()` / `queueGuestReport` / `startUpload` / `saveReportOffline` / `finishAfterOfflineSave` / `notifyTargetedFoundSubmitted` / `submitItemToBackend` / `setSubmitting`
```java
private void validateAndSubmit() {
    // require title/description/category/location/(verification if found);
    // guest → queueGuestReport; else pin or forwardGeocode then startUpload
}

private void startUpload(...) {
    // if offline new report → saveReportOffline; else upload image; submitItemToBackend
}

private void submitItemToBackend(String authHeader, Item item, String imageUrl) {
    // updateItem or createItem; offline fallback enqueue; notifyTargetedFoundSubmitted; finish
}
```
- **What it does:** Validates the form, then either queues a guest/offline report or uploads the photo and creates/updates the item on the backend.
- **Where it's called:** Submit button → validate; helpers call each other on success/failure paths.
- **Why it's written this way:** Many offline/network-edge cases: no auth (guest queue), no pin (forward geocode), backend down (PendingReportStore), image upload failure. Targeted found marks the lost item interacted and sets RESULT_OK. Offline success uses `finishAffinity()` after a “saved” toast.
- **Step by step:**
  1. Validate required fields; found requires verification question.
  2. No auth → `queueGuestReport` (optional geocode, then offline save with null user id).
  3. With auth: if no pin, geocode on executor; on failure maybe offline; else `startUpload`.
  4. `startUpload`: offline short-circuit; upload image if Uri; build Item; `submitItemToBackend`.
  5. Backend success: toast, notify targeted found, `finish`. Failure/offline: enqueue and `finishAfterOfflineSave`.

### `protected void onCreate` / `onDestroy` / `onSupportNavigateUp`
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    // inflate; setup all sections; read extras; maybe loadExistingItem
}

@Override
protected void onDestroy() {
    super.onDestroy();
    uploadExecutor.shutdownNow();
}

@Override
public boolean onSupportNavigateUp() {
    getOnBackPressedDispatcher().onBackPressed();
    return true;
}
```
- **What it does:** Builds the form, applies Intent mode (edit / lock found), shuts down the executor, handles up navigation.
- **Where it's called:** Lifecycle / system. Opened from dashboard report entry, `MyItemsFragment` edit, and `ItemDetailsActivity` “I found this.”
- **Why it's written this way:** `lockFoundType` is true if the extra is set OR a target lost id is present.
- **Step by step:**
  1. Inflate; wire toolbar, category, image, location, chips, submit.
  2. Read extras; apply found lock; if editing, set title and load item.
  3. Destroy shuts down uploadExecutor; up triggers back.
