```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/md_theme_background"
    android:fitsSystemWindows="true"
    android:orientation="vertical">
```

What that function does: I am the edit-profile screen.
What the function is responsible for: I am the root of activity_edit_profile.
Where it works: EditProfileActivity inflates me through ActivityEditProfileBinding.
Why I chose to write the code of the function the way its written: fitsSystemWindows keeps the toolbar under the status bar.
More: I use a vertical LinearLayout.

```xml
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar_edit_profile"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@color/md_theme_background"
        app:navigationIcon="@drawable/ic_arrow_back_24"
        app:navigationIconTint="@color/text_primary"
        app:title="@string/edit_profile_title"
        app:titleCentered="true"
        app:titleTextAppearance="@style/TextAppearance.ReClaim.SettingsScreenTitle"
        app:titleTextColor="@color/text_primary" />
```

What that function does: I am a light centered title with a back arrow.
What the function is responsible for: I am the edit-profile toolbar.
Where it works: EditProfileActivity uses toolbar_edit_profile for up navigation.
Why I chose to write the code of the function the way its written: Centered title matches other settings screens, not the dark auth hero.
More: I use titleCentered.

```xml
            <FrameLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:layout_marginTop="@dimen/space_16">

                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/card_avatar"
                    android:layout_width="@dimen/space_104"
                    android:layout_height="@dimen/space_104"
                    android:clickable="true"
                    android:focusable="true"
                    app:cardBackgroundColor="@color/md_theme_primaryContainer"
                    app:cardCornerRadius="@dimen/space_52"
                    app:cardElevation="0dp"
                    app:strokeWidth="0dp">

                    <ImageView
                        android:id="@+id/image_profile_avatar"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:contentDescription="@string/cd_profile_avatar"
                        android:padding="@dimen/space_28"
                        android:scaleType="centerCrop"
                        android:src="@drawable/ic_settings_person"
                        app:tint="@color/brand_dark" />
                </com.google.android.material.card.MaterialCardView>
```

What that function does: I am the avatar picker with a camera badge and change-photo button.
What the function is responsible for: I take a new profile picture.
Where it works: EditProfileActivity wires card_avatar and btn_change_photo.
Why I chose to write the code of the function the way its written: Same pattern as register so the two screens feel related.
More: I use a round MaterialCardView.

```xml
                    <com.google.android.material.textfield.TextInputLayout
                        android:id="@+id/input_layout_name"
                        style="@style/Widget.ReClaim.TextInput"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:hint="@string/hint_profile_name"
                        app:startIconDrawable="@drawable/ic_settings_person">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/edit_name"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:inputType="textPersonName|textCapWords"
                            android:textColor="@color/text_primary" />
                    </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the name field.
What the function is responsible for: I take the new display name.
Where it works: EditProfileActivity reads edit_name.
Why I chose to write the code of the function the way its written: Cap words fits a person's name.
More: I use ic_settings_person.

```xml
                    <com.google.android.material.textfield.TextInputLayout
                        android:id="@+id/input_layout_email"
                        style="@style/Widget.ReClaim.TextInput"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="@dimen/space_12"
                        android:hint="@string/hint_email"
                        app:startIconDrawable="@android:drawable/ic_dialog_email">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/edit_email"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:inputType="textEmailAddress"
                            android:textColor="@color/text_primary" />
                    </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the email field.
What the function is responsible for: I take the new email.
Where it works: EditProfileActivity reads edit_email.
Why I chose to write the code of the function the way its written: Email keyboard matches signup.
More: I use Widget.ReClaim.TextInput.

```xml
                    <com.google.android.material.textfield.TextInputLayout
                        android:id="@+id/input_layout_phone"
                        style="@style/Widget.ReClaim.TextInput"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="@dimen/space_12"
                        android:hint="@string/hint_profile_phone"
                        app:startIconDrawable="@android:drawable/ic_menu_call">

                        <com.google.android.material.textfield.TextInputEditText
                            android:id="@+id/edit_phone"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:inputType="phone"
                            android:textColor="@color/text_primary" />
                    </com.google.android.material.textfield.TextInputLayout>
```

What that function does: I am the phone field.
What the function is responsible for: I take the new phone.
Where it works: EditProfileActivity reads edit_phone.
Why I chose to write the code of the function the way its written: inputType phone brings the number pad.
More: I use ic_menu_call.

```xml
            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_save_profile"
                style="@style/Widget.ReClaim.Button.Primary"
                android:layout_width="match_parent"
                android:layout_height="@dimen/button_height"
                android:layout_marginTop="@dimen/space_20"
                android:text="@string/btn_save_profile" />

            <ProgressBar
                android:id="@+id/progress"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:layout_marginTop="@dimen/space_16"
                android:indeterminateTint="@color/brand_accent"
                android:visibility="gone" />
```

What that function does: I am save and a spinner while the update runs.
What the function is responsible for: I submit profile changes.
Where it works: EditProfileActivity wires btn_save_profile and shows progress.
Why I chose to write the code of the function the way its written: Spinner starts gone.
More: I use Widget.ReClaim.Button.Primary.
