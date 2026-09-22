# activity_report.xml

## Overview
This layout is the "create/edit a report" screen: choose lost vs found, add a photo,
fill in title/description/category, pick a location (text field + "use current" +
a map), optionally add a verification question, and submit. It is inflated by
`ReportActivity` through `ActivityReportBinding`.

Key idea: this is a long scrolling form; the whole screen is wrapped in a `ScrollView`
so it fits on any phone. See `docs/GLOSSARY.md`.

## Concepts used here
- **ScrollView** — the single scrolling wrapper for the whole form. See `docs/GLOSSARY.md`.
- **LinearLayout** — vertical stacks for the form and inside each card. See `docs/GLOSSARY.md`.
- **MaterialToolbar** — the top bar with a back arrow and title. See `docs/GLOSSARY.md`.
- **MaterialCardView** — groups related fields into rounded cards. See `docs/GLOSSARY.md`.
- **ChipGroup + Chip** — the single-choice Lost/Found selector. See `docs/GLOSSARY.md`.
- **FrameLayout** — stacks the photo preview over its placeholder. See `docs/GLOSSARY.md`.
- **TextInputLayout + TextInputEditText / AutoCompleteTextView** — the text fields and
  the category/location dropdowns. See `docs/GLOSSARY.md`.
- **FragmentContainerView** — hosts the Google Map fragment. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityReportBinding`. See `docs/GLOSSARY.md`.

## Screen structure
The root is a `ScrollView` (which can only have one child), so a `LinearLayout` inside
it holds everything:

```
ScrollView (root)
└── LinearLayout (vertical)
    ├── MaterialToolbar (toolbar_report)
    └── LinearLayout (padded form)
        ├── subtitle
        ├── Card: report type (ChipGroup: chip_type_found / chip_type_lost)
        ├── Card: photo capture (image_preview over layout_photo_placeholder)
        ├── "Choose photo" text button
        ├── Card: title + description + category dropdown
        ├── Card: location field + "use current" + map picker
        ├── Card: verification question (shown for Found items)
        └── Submit button
```

The root is a `ScrollView` (not a `NestedScrollView`) because this screen is a simple
top-level scroller with no collapsing app bar to coordinate with.

## Components

### Toolbar
```xml
<com.google.android.material.appbar.MaterialToolbar
    android:id="@+id/toolbar_report"
    android:background="@color/brand_dark"
    app:navigationIcon="@drawable/ic_arrow_back_24"
    app:title="@string/report_title"
    android:paddingTop="@dimen/space_60" />
```
- **What it shows:** a dark bar with a back arrow and the "Report" title.
- **Why it's set up this way:** the top padding pushes the title below the status bar
  (this screen doesn't use `fitsSystemWindows`). It uses the brand dark color for
  contrast with the light form.
- **How the code uses it:** `ReportActivity` calls
  `setSupportActionBar(binding.toolbarReport)` so it acts as the screen's app bar.

### Report type selector (Lost / Found)
```xml
<com.google.android.material.chip.ChipGroup
    android:id="@+id/chip_group_report_type"
    app:checkedChip="@+id/chip_type_found"
    app:selectionRequired="true"
    app:singleSelection="true">
    <com.google.android.material.chip.Chip android:id="@+id/chip_type_found" android:checked="true" ... />
    <com.google.android.material.chip.Chip android:id="@+id/chip_type_lost" ... />
</com.google.android.material.chip.ChipGroup>
```
- **What it shows:** two selectable chips, "Found" and "Lost", inside a card, with
  "Found" checked by default.
- **Why it's set up this way:** `singleSelection="true"` and `selectionRequired="true"`
  make it behave like radio buttons — exactly one is always chosen. This is the key field
  because it changes the rest of the form (only found items ask a verification question).
- **How the code uses it:** the code reads `binding.chipTypeLost.isChecked()` to decide
  the type, listens on `binding.chipGroupReportType` to show/hide the verification card,
  and can lock the selection to "Found" (e.g. when responding to someone's lost item).

### Photo capture card
```xml
<com.google.android.material.card.MaterialCardView android:id="@+id/card_image_capture" ...>
    <FrameLayout>
        <ImageView android:id="@+id/image_preview" android:visibility="gone" ... />
        <LinearLayout android:id="@+id/layout_photo_placeholder" android:layout_gravity="center">
            <ImageView android:src="@android:drawable/ic_menu_camera" />
            <com.google.android.material.textview.MaterialTextView android:text="@string/btn_take_photo" />
        </LinearLayout>
    </FrameLayout>
</com.google.android.material.card.MaterialCardView>

<com.google.android.material.button.MaterialButton android:id="@+id/btn_choose_photo" ... />
```
- **What it shows:** a tappable card that shows a camera icon and "Take photo" until a
  picture is chosen; then it shows the photo. A separate "Choose photo" button picks from
  the gallery.
- **Why it's set up this way:** a `FrameLayout` stacks the preview image on top of the
  placeholder so the app can just swap which one is visible instead of rebuilding the
  view. The whole card is clickable to feel like one big shutter button.
- **How the code uses it:** `binding.cardImageCapture` and `binding.imagePreview` open a
  camera/gallery chooser; `binding.btnChoosePhoto` opens the gallery. When a photo is
  set, the code hides `binding.layoutPhotoPlaceholder`, shows `binding.imagePreview`, and
  loads the image into it with Glide.

### Title, description, and category
```xml
<com.google.android.material.textfield.TextInputLayout android:id="@+id/input_layout_item_title" ...>
    <com.google.android.material.textfield.TextInputEditText android:id="@+id/edit_item_title" android:inputType="textCapSentences" />
</com.google.android.material.textfield.TextInputLayout>

<com.google.android.material.textfield.TextInputLayout android:id="@+id/input_layout_description" ...>
    <com.google.android.material.textfield.TextInputEditText android:id="@+id/edit_description" android:inputType="textMultiLine|textCapSentences" android:minLines="4" />
</com.google.android.material.textfield.TextInputLayout>

<com.google.android.material.textfield.TextInputLayout android:id="@+id/input_layout_category" app:endIconMode="dropdown_menu">
    <AutoCompleteTextView android:id="@+id/dropdown_category" android:inputType="none" ... />
</com.google.android.material.textfield.TextInputLayout>
```
- **What it shows:** a card with the item title (single line), a multi-line description,
  and a category dropdown.
- **Why it's set up this way:** each field is a `TextInputLayout` wrapping its input so
  hints float and errors appear under the box. The description sets `minLines="4"` for a
  taller box; the category uses an `AutoCompleteTextView` with `inputType="none"` and
  `endIconMode="dropdown_menu"` so it behaves like a pick-from-list menu, not free text.
- **How the code uses it:** the code reads `binding.editItemTitle`,
  `binding.editDescription`, and `binding.dropdownCategory`, showing errors on the
  matching `inputLayout*` when empty. The category list is attached with
  `binding.dropdownCategory.setAdapter(...)` (using `item_dropdown_category.xml` rows).
  When editing an existing report, the code pre-fills all three.

### Location field, "use current location", and map
```xml
<com.google.android.material.textfield.TextInputLayout android:id="@+id/input_layout_location" app:endIconMode="custom">
    <com.google.android.material.textfield.MaterialAutoCompleteTextView
        android:id="@+id/edit_location"
        android:completionThreshold="3"
        android:inputType="textPostalAddress|textCapSentences" />
</com.google.android.material.textfield.TextInputLayout>

<com.google.android.material.button.MaterialButton android:id="@+id/btn_use_current_location" ... />

<com.google.android.material.card.MaterialCardView android:id="@+id/card_map_container" ...>
    <com.example.reclaim.ui.report.MapTouchWrapper>
        <androidx.fragment.app.FragmentContainerView
            android:id="@+id/map_picker"
            android:name="com.google.android.gms.maps.SupportMapFragment" />
    </com.example.reclaim.ui.report.MapTouchWrapper>
</com.google.android.material.card.MaterialCardView>
```
- **What it shows:** a location text field with address autocomplete, a "Use current
  location" button, and a Google Map inside a rounded card for picking the spot.
- **Why it's set up this way:** `MaterialAutoCompleteTextView` with
  `completionThreshold="3"` shows address suggestions after three characters. The map is
  a `FragmentContainerView` (the standard way to embed a map fragment) wrapped in a
  `MapTouchWrapper` so the map's own touch gestures don't fight the page's scroll. The
  rounded card is just a nice frame around the map.
- **How the code uses it:** the code attaches the places autocomplete adapter to
  `binding.editLocation`, handles picking a suggestion, wires
  `binding.btnUseCurrentLocation` to fill the address from GPS, and reads
  `binding.editLocation` at submit time (showing errors on `binding.inputLayoutLocation`).

### Verification question card
```xml
<com.google.android.material.card.MaterialCardView android:id="@+id/card_verification" ...>
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/input_layout_verification"
        app:helperText="@string/hint_verification_hint"
        app:helperTextEnabled="true">
        <com.google.android.material.textfield.TextInputEditText android:id="@+id/edit_verification" ... />
    </com.google.android.material.textfield.TextInputLayout>
</com.google.android.material.card.MaterialCardView>
```
- **What it shows:** a card with a "verification question" field and a small helper line
  under it (a question only the true owner could answer).
- **Why it's set up this way:** `helperTextEnabled` shows guidance text under the field.
  This whole card is hidden for lost items and shown for found ones.
- **How the code uses it:** the code toggles `binding.cardVerification` visibility based
  on the Lost/Found chip, reads `binding.editVerification` for found items, and shows an
  error on `binding.inputLayoutVerification` if it's required but empty.

### Submit button
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_submit_report"
    style="@style/Widget.ReClaim.Button.Primary"
    android:text="@string/btn_submit_report" />
```
- **What it shows:** the full-width primary button that sends the report.
- **Why it's set up this way:** full width and primary styling make it the clear final
  action at the bottom of the form.
- **How the code uses it:** `binding.btnSubmitReport` runs the validate-and-submit flow;
  while submitting it is disabled and its text changes to a progress label.
