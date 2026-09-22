# activity_register.xml

## Overview
This layout is the sign-up screen: the same dark hero as login on top, then a light
scrolling form for profile photo, name, email, phone, password, and confirm password.
It is inflated by `RegisterActivity` through `ActivityRegisterBinding`
(`ActivityRegisterBinding.inflate(...)` then `setContentView(binding.getRoot())`).

Key idea: a **layout XML** describes the views; `RegisterActivity` (an **Activity**,
one full screen) fills them in and reads them. See `docs/GLOSSARY.md`.

## Concepts used here
- **LinearLayout** — stacks children vertically (root and form). See `docs/GLOSSARY.md`.
- **NestedScrollView** — makes the tall form scroll. See `docs/GLOSSARY.md`.
- **FrameLayout** — stacks views on top of each other; used to place a small camera
  badge over the avatar circle. See `docs/GLOSSARY.md`.
- **MaterialCardView** — a rounded card; here it makes the circular avatar and the tiny
  camera badge. See `docs/GLOSSARY.md`.
- **TextInputLayout + TextInputEditText** — the Material text fields. See `docs/GLOSSARY.md`.
- **Material Button** — the register button and the text buttons. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityRegisterBinding` for typed access to the ids. See `docs/GLOSSARY.md`.

## Screen structure
A vertical `LinearLayout` with a fixed hero and a scrollable form:

```
LinearLayout (root, vertical)
├── LinearLayout (hero: logo + title + subtitle)
└── NestedScrollView
    └── LinearLayout (vertical form)
        ├── title text
        ├── FrameLayout (avatar circle + camera badge)
        ├── "Add photo" text button
        ├── full name field
        ├── email field
        ├── phone field
        ├── password field
        ├── confirm password field
        ├── Register button
        └── "Have an account? Log in" row
```

The root is a `LinearLayout` for the same reason as login: two blocks stacked in a
column (fixed hero + flexible form).

## Components

### Root and hero banner
```xml
<LinearLayout
    android:background="@color/brand_deep"
    android:fitsSystemWindows="true"
    android:orientation="vertical">

    <LinearLayout
        android:layout_height="@dimen/space_200"
        android:gravity="center"
        android:orientation="vertical">
        <ImageView android:id="@+id/image_logo" ... />
        <com.google.android.material.textview.MaterialTextView android:id="@+id/text_register_title" ... />
        <com.google.android.material.textview.MaterialTextView android:id="@+id/text_register_subtitle" ... />
    </LinearLayout>
```
- **What it shows:** the dark brand header with logo, app name, and a "create your
  account" subtitle.
- **Why it's set up this way:** matches the login hero so the two auth screens feel
  like one flow; `fitsSystemWindows="true"` keeps it clear of the status bar.
- **How the code uses it:** these ids are static; `RegisterActivity` does not touch them.

### Avatar picker (circle + camera badge)
```xml
<FrameLayout
    android:layout_gravity="center_horizontal">

    <com.google.android.material.card.MaterialCardView
        android:id="@+id/card_avatar"
        android:clickable="true"
        app:cardCornerRadius="@dimen/space_48">
        <ImageView
            android:id="@+id/image_profile_avatar"
            android:src="@drawable/ic_settings_person" />
    </com.google.android.material.card.MaterialCardView>

    <com.google.android.material.card.MaterialCardView
        android:layout_gravity="bottom|end"
        app:cardBackgroundColor="@color/brand_accent">
        <ImageView android:src="@android:drawable/ic_menu_camera" />
    </com.google.android.material.card.MaterialCardView>
</FrameLayout>

<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_add_photo"
    android:text="@string/register_add_photo" />
```
- **What it shows:** a round avatar (person icon until a photo is chosen) with a small
  accent-colored camera badge in the bottom-right corner, plus an "Add photo" button.
- **Why it's set up this way:** a `FrameLayout` overlays the badge on top of the avatar
  circle. The circle is a `MaterialCardView` with a large corner radius (half its size),
  which is the easy way to make a square image look round.
- **How the code uses it:** `binding.cardAvatar` and `binding.btnAddPhoto` both open the
  photo picker. After a photo is picked, the code clears the icon padding/tint and loads
  the image into `binding.imageProfileAvatar` (with Glide). `btnAddPhoto` also shows an
  "uploading…" state while the photo uploads.

### Name / Email / Phone fields
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/input_layout_full_name"
    android:hint="@string/hint_full_name"
    app:startIconDrawable="@android:drawable/ic_menu_myplaces">
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/edit_full_name"
        android:inputType="textPersonName" />
</com.google.android.material.textfield.TextInputLayout>

<!-- email field: id input_layout_email / edit_email, inputType textEmailAddress -->
<!-- phone field: id input_layout_phone / edit_phone, inputType phone -->
```
- **What it shows:** three stacked fields for full name, email, and phone, each with a
  leading icon and hint.
- **Why it's set up this way:** each field is one `TextInputLayout` wrapping one edit
  text so the hint floats up and errors appear under the box. The `inputType` differs per
  field so Android shows the right keyboard: a name keyboard, an email keyboard, and a
  phone dialpad.
- **How the code uses it:** `RegisterActivity.attemptRegistration()` reads
  `binding.editFullName`, `binding.editEmail`, and `binding.editPhone`, and reports
  problems on the matching `inputLayout*` (for example `binding.inputLayoutEmail.setError(...)`
  when the email is empty or invalid).

### Password and Confirm password fields
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/input_layout_password"
    android:hint="@string/hint_password"
    app:endIconMode="password_toggle">
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/edit_password"
        android:inputType="textPassword" />
</com.google.android.material.textfield.TextInputLayout>

<!-- confirm: id input_layout_confirm_password / edit_confirm_password, same toggle -->
```
- **What it shows:** two masked password fields, each with a show/hide eye.
- **Why it's set up this way:** two separate fields let the app check that the password
  and its confirmation match before creating the account; `password_toggle` helps the
  user avoid typos they can't see.
- **How the code uses it:** the code reads `binding.editPassword` and
  `binding.editConfirmPassword`, enforces a minimum length, and shows
  "passwords don't match" on `binding.inputLayoutConfirmPassword` when they differ.

### Register button and "Log in" row
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_register"
    style="@style/Widget.ReClaim.Button.Primary"
    android:text="@string/btn_register" />

<LinearLayout android:orientation="horizontal" android:gravity="center">
    <com.google.android.material.textview.MaterialTextView android:id="@+id/text_has_account" ... />
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_go_login"
        style="@style/Widget.Material3.Button.TextButton"
        android:text="@string/btn_go_login" />
</LinearLayout>
```
- **What it shows:** the full-width primary "Create account" button, then a centered
  "Already have an account? Log in" row.
- **Why it's set up this way:** the primary button is the main action; the log-in link is
  a quiet text button so it doesn't compete with it.
- **How the code uses it:** `binding.btnRegister` runs `attemptRegistration()`;
  `binding.btnGoLogin` calls `finish()` to go back to login. While registering, the code
  disables all the fields and buttons and switches the register button text to
  "Creating account…".
