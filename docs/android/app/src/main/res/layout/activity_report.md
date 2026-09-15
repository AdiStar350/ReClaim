```xml
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/md_theme_background"
    android:fillViewport="true">
```

What that function does: I am the lost/found report form.
What the function is responsible for: I am the root of activity_report.
Where it works: ReportActivity inflates me through ActivityReportBinding.
Why I chose to write the code of the function the way its written: One ScrollView holds type, photo, details, map, and submit. MapTouchWrapper stops the scroll from eating map drags.
More: I use fillViewport true.

```xml
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar_report"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/brand_dark"
            app:navigationIcon="@drawable/ic_arrow_back_24"
            app:navigationIconTint="@color/brand_cream"
            app:title="@string/report_title"
            app:titleTextColor="@color/brand_cream"
            android:paddingTop="@dimen/space_60" />
```

What that function does: I am the report toolbar with extra top padding for the status bar.
What the function is responsible for: I show New report, Edit, or Found for lost.
Where it works: ReportActivity.setupToolbar uses me. Edit and found-lock change the title in Java.
Why I chose to write the code of the function the way its written: paddingTop 60 covers the status bar because this screen does not use fitsSystemWindows on the root.
More: I use brand_dark.

```xml
                    <com.google.android.material.chip.ChipGroup
                        android:id="@+id/chip_group_report_type"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="@dimen/space_10"
                        app:checkedChip="@+id/chip_type_found"
                        app:selectionRequired="true"
                        app:singleSelection="true">

                        <com.google.android.material.chip.Chip
                            android:id="@+id/chip_type_found"
                            style="@style/Widget.ReClaim.Chip.Filter"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:checked="true"
                            android:text="@string/type_found" />

                        <com.google.android.material.chip.Chip
                            android:id="@+id/chip_type_lost"
                            style="@style/Widget.ReClaim.Chip.Filter"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/type_lost" />

                    </com.google.android.material.chip.ChipGroup>
```

What that function does: I am lost or found. Found starts checked.
What the function is responsible for: I set the report type.
Where it works: ReportActivity.setupReportTypeChips listens to me. I-found-this disables chip_type_lost.
Why I chose to write the code of the function the way its written: singleSelection keeps one type. Found default matches a finder filling the form.
More: I use Widget.ReClaim.Chip.Filter.

```xml
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/card_image_capture"
                style="@style/Widget.ReClaim.Card"
                android:layout_width="match_parent"
                android:layout_height="@dimen/space_220"
                android:layout_marginTop="@dimen/space_16"
                android:clickable="true"
                android:focusable="true"
                app:cardBackgroundColor="@color/md_theme_surfaceContainerLow">
```

What that function does: I am the photo drop zone. Placeholder first, then preview.
What the function is responsible for: I open camera or gallery.
Where it works: ReportActivity.setupImageCapture wires card_image_capture, image_preview, and btn_choose_photo.
Why I chose to write the code of the function the way its written: A tall card is an easy tap target. Preview starts gone.
More: layout_photo_placeholder sits in the center until a photo loads.

```xml
            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_choose_photo"
                style="@style/Widget.Material3.Button.TextButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="end"
                android:layout_marginTop="@dimen/space_8"
                android:text="@string/btn_choose_photo"
                android:textColor="@color/brand_accent"
                app:icon="@android:drawable/ic_menu_gallery" />
```

What that function does: I skip the chooser and open the gallery.
What the function is responsible for: I am choose photo.
Where it works: ReportActivity.launchGallery.
Why I chose to write the code of the function the way its written: A text button on the right stays secondary to the big card.
More: I use ic_menu_gallery.

```xml
                    <com.google.android.material.textfield.TextInputLayout
                        android:id="@+id/input_layout_item_title"
                        style="@style/Widget.ReClaim.TextInput"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="@string/hint_item_title">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/edit_item_title"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:inputType="textCapSentences" />

                    </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the item title field.
What the function is responsible for: I take the report title. Errors go on the layout.
Where it works: ReportActivity.validateAndSubmit reads edit_item_title.
Why I chose to write the code of the function the way its written: Cap sentences helps a short name.
More: I use Widget.ReClaim.TextInput.

```xml
                    <com.google.android.material.textfield.TextInputLayout
                        android:id="@+id/input_layout_description"
                        style="@style/Widget.ReClaim.TextInput"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="@dimen/space_14"
                        android:hint="@string/hint_item_description">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/edit_description"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:gravity="top"
                            android:inputType="textMultiLine|textCapSentences"
                            android:minLines="4" />

                    </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the description field.
What the function is responsible for: I take a longer write-up.
Where it works: ReportActivity.validateAndSubmit reads edit_description.
Why I chose to write the code of the function the way its written: minLines 4 leaves room without a huge box.
More: I use textMultiLine.

```xml
                    <com.google.android.material.textfield.TextInputLayout
                        android:id="@+id/input_layout_category"
                        style="@style/Widget.ReClaim.TextInput"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="@dimen/space_14"
                        android:hint="@string/hint_item_category"
                        app:endIconMode="dropdown_menu">

                        <AutoCompleteTextView
                            android:id="@+id/dropdown_category"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:inputType="none"
                            android:paddingStart="@dimen/space_15"
                            android:popupBackground="@color/md_theme_surfaceContainerLowest"
                            android:textColor="@color/text_primary" />

                    </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the category dropdown.
What the function is responsible for: I pick a category from the list.
Where it works: ReportActivity.setupCategoryDropdown uses item_dropdown_category.xml.
Why I chose to write the code of the function the way its written: inputType none stops typing a free category.
More: I use endIconMode dropdown_menu.

```xml
                    <com.google.android.material.textfield.TextInputLayout
                        android:id="@+id/input_layout_location"
                        style="@style/Widget.ReClaim.TextInput"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="@string/hint_item_location"
                        app:endIconDrawable="@android:drawable/ic_dialog_map"
                        app:endIconMode="custom">

                        <com.google.android.material.textfield.MaterialAutoCompleteTextView
                            android:id="@+id/edit_location"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:completionThreshold="3"
                            android:dropDownSelector="@android:color/transparent"
                            android:imeOptions="actionDone"
                            android:inputType="textPostalAddress|textCapSentences"
                            android:paddingStart="@dimen/space_15"
                            android:popupBackground="@drawable/bg_popup_menu"
                            android:popupTheme="@style/ThemeOverlay.ReClaim.PopupMenu"
                            android:textColor="@color/text_primary" />

                    </com.google.android.material.textfield.TextInputLayout>

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btn_use_current_location"
                        style="@style/Widget.Material3.Button.TonalButton"
                        android:layout_width="match_parent"
                        android:layout_height="@dimen/space_48"
                        android:layout_marginTop="@dimen/space_12"
                        android:text="@string/btn_use_current_location"
                        app:icon="@android:drawable/ic_menu_mylocation" />
```

What that function does: I am the address field with Places suggestions, plus use-current-location.
What the function is responsible for: I take a place by typing, pick, or GPS.
Where it works: ReportActivity.setupPlacesAutocomplete binds edit_location. btn_use_current_location calls fetchCurrentLocation.
Why I chose to write the code of the function the way its written: Threshold 3 cuts empty suggestions. A map icon is only decoration; the map below is the picker.
More: I use PlacesAutocompleteAdapter.

```xml
                    <com.google.android.material.card.MaterialCardView
                        android:id="@+id/card_map_container"
                        android:layout_width="match_parent"
                        android:layout_height="@dimen/space_400"
                        android:layout_marginTop="@dimen/space_10"
                        app:cardCornerRadius="@dimen/space_18"
                        app:cardElevation="0dp"
                        app:strokeColor="@color/md_theme_outlineVariant"
                        app:strokeWidth="@dimen/space_1">

                        <com.example.reclaim.ui.report.MapTouchWrapper
                            android:layout_width="match_parent"
                            android:layout_height="match_parent">

                            <androidx.fragment.app.FragmentContainerView
                                android:id="@+id/map_picker"
                                android:name="com.google.android.gms.maps.SupportMapFragment"
                                android:layout_width="match_parent"
                                android:layout_height="match_parent" />

                        </com.example.reclaim.ui.report.MapTouchWrapper>

                    </com.google.android.material.card.MaterialCardView>
```

What that function does: I am the map picker in a rounded card.
What the function is responsible for: I let the user tap a pin.
Where it works: ReportActivity.onMapReady uses map_picker. MapTouchWrapper asks the parent ScrollView not to steal the drag.
Why I chose to write the code of the function the way its written: 400dp is tall enough to pan. The wrapper is the only custom view in this form.
More: I use SupportMapFragment.

```xml
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/card_verification"
                style="@style/Widget.ReClaim.Card"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_16">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="@dimen/space_18">

                    <com.google.android.material.textfield.TextInputLayout
                        android:id="@+id/input_layout_verification"
                        style="@style/Widget.ReClaim.TextInput"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="@string/hint_verification_question"
                        app:helperText="@string/hint_verification_hint"
                        app:helperTextEnabled="true">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/edit_verification"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:inputType="textCapSentences" />

                    </com.google.android.material.textfield.TextInputLayout>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>
```

What that function does: I am the check-question field for found reports.
What the function is responsible for: I take a question a claimant must answer.
Where it works: ReportActivity.updateVerificationVisibility hides me on lost. validateAndSubmit reads edit_verification.
Why I chose to write the code of the function the way its written: Helper text explains why the question exists.
More: I use helperTextEnabled.

```xml
            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_submit_report"
                style="@style/Widget.ReClaim.Button.Primary"
                android:layout_width="match_parent"
                android:layout_height="@dimen/button_height"
                android:layout_marginTop="@dimen/space_20"
                android:text="@string/btn_submit_report" />
```

What that function does: I am the submit button.
What the function is responsible for: I start validation.
Where it works: ReportActivity.setupSubmitButton wires me. setSubmitting changes my text.
Why I chose to write the code of the function the way its written: Full width primary at the end of the form.
More: I use Widget.ReClaim.Button.Primary.
