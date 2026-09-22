# activity_edit_profile.xml

## Overview
This layout is the "edit your profile" screen: a centered toolbar, an avatar you can
change, a card with name/email/phone fields, a save button, and a spinner. It is
inflated by `EditProfileActivity` through `ActivityEditProfileBinding`.

Key idea: one **Activity** screen whose fields the Activity pre-fills from the current
user and reads back on save. See `docs/GLOSSARY.md`.

## Concepts used here
- **LinearLayout** — vertical stack for the screen and inside the card. See `docs/GLOSSARY.md`.
- **MaterialToolbar** — the centered title bar with a back arrow. See `docs/GLOSSARY.md`.
- **NestedScrollView** — scrolls the form. See `docs/GLOSSARY.md`.
- **FrameLayout + MaterialCardView** — the avatar circle with a camera badge. See `docs/GLOSSARY.md`.
- **TextInputLayout + TextInputEditText** — the name/email/phone fields. See `docs/GLOSSARY.md`.
- **Material Button** — the save and change-photo buttons. See `docs/GLOSSARY.md`.
- **ProgressBar** — spinner during the save. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityEditProfileBinding`. See `docs/GLOSSARY.md`.

## Screen structure
A vertical `LinearLayout`:

```
LinearLayout (root, vertical, light background)
├── MaterialToolbar (toolbar_edit_profile, centered title)
└── NestedScrollView
    └── LinearLayout
        ├── FrameLayout (avatar circle + camera badge)
        ├── "Change photo" text button
        ├── Card: name field + email field + phone field
        ├── Save button
        └── ProgressBar (hidden)
```

The root is a `LinearLayout` because the toolbar and the scrolling form are just stacked
in a column.

## Components

### Toolbar
```xml
<com.google.android.material.appbar.MaterialToolbar
    android:id="@+id/toolbar_edit_profile"
    app:navigationIcon="@drawable/ic_arrow_back_24"
    app:title="@string/edit_profile_title"
    app:titleCentered="true" />
```
- **What it shows:** a light bar with a back arrow and a centered "Edit profile" title.
- **Why it's set up this way:** `titleCentered="true"` matches the app's settings-style
  screens; the light background matches the form.
- **How the code uses it:** `binding.toolbarEditProfile.setNavigationOnClickListener(...)`
  triggers the back navigation.

### Avatar picker (circle + camera badge)
```xml
<FrameLayout android:layout_gravity="center_horizontal">
    <com.google.android.material.card.MaterialCardView
        android:id="@+id/card_avatar"
        android:clickable="true"
        app:cardCornerRadius="@dimen/space_52">
        <ImageView android:id="@+id/image_profile_avatar" android:src="@drawable/ic_settings_person" ... />
    </com.google.android.material.card.MaterialCardView>
    <com.google.android.material.card.MaterialCardView app:cardBackgroundColor="@color/brand_accent">
        <ImageView android:src="@android:drawable/ic_menu_camera" />
    </com.google.android.material.card.MaterialCardView>
</FrameLayout>

<com.google.android.material.button.MaterialButton android:id="@+id/btn_change_photo" ... />
```
- **What it shows:** a round avatar with a small accent camera badge, plus a
  "Change photo" button.
- **Why it's set up this way:** the same pattern as register — a `FrameLayout` overlays
  the badge on a `MaterialCardView` circle (corner radius = half the size makes it round).
- **How the code uses it:** `binding.cardAvatar` and `binding.btnChangePhoto` open the
  photo picker; a chosen photo is loaded into `binding.imageProfileAvatar` (Glide, with
  padding/tint cleared so it fills the circle).

### Name / Email / Phone card
```xml
<com.google.android.material.card.MaterialCardView style="@style/Widget.ReClaim.SettingsCard">
    <LinearLayout android:orientation="vertical" android:padding="@dimen/space_16">
        <com.google.android.material.textfield.TextInputLayout android:id="@+id/input_layout_name" ...>
            <com.google.android.material.textfield.TextInputEditText android:id="@+id/edit_name" android:inputType="textPersonName|textCapWords" />
        </com.google.android.material.textfield.TextInputLayout>
        <!-- email: input_layout_email / edit_email, inputType textEmailAddress -->
        <!-- phone: input_layout_phone / edit_phone, inputType phone -->
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```
- **What it shows:** a settings-style card holding the three editable fields with leading
  icons and hints.
- **Why it's set up this way:** grouping the fields in one card matches the profile
  settings look; each field is a `TextInputLayout` so hints float and errors show. The
  `inputType`s pick the right keyboard per field.
- **How the code uses it:** on load, the code fills `binding.editName`,
  `binding.editEmail`, and `binding.editPhone` from the current user. On save it reads
  them back; if the name is empty it shows an error on `binding.inputLayoutName`. (The
  email and phone layouts don't get validation errors in the current code.)

### Save button and spinner
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_save_profile"
    style="@style/Widget.ReClaim.Button.Primary"
    android:text="@string/btn_save_profile" />

<ProgressBar android:id="@+id/progress" android:visibility="gone" />
```
- **What it shows:** the full-width save button, with a hidden spinner below it.
- **Why it's set up this way:** the spinner is `gone` until saving starts, so it doesn't
  reserve space; the primary button is the main action.
- **How the code uses it:** `binding.btnSaveProfile` runs `saveProfile()`. While saving,
  the code shows `binding.progress` and disables the save button, change-photo button,
  and avatar.
