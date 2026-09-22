# activity_reset_password.xml

## Overview
This layout is the "set a new password" screen, shown at the end of the forgot-password
flow (after the emailed code is verified). It has a back-arrow toolbar, a small hero,
and a form with two password fields and an update button. It is inflated by
`ResetPasswordActivity` through `ActivityResetPasswordBinding`.

Key idea: this is one **Activity** screen; its **layout XML** holds the fields the
Activity reads. See `docs/GLOSSARY.md`.

## Concepts used here
- **LinearLayout** — vertical stack for the whole screen. See `docs/GLOSSARY.md`.
- **MaterialToolbar** — the top bar that holds the back arrow. See `docs/GLOSSARY.md`.
- **NestedScrollView** — lets the form scroll on small screens. See `docs/GLOSSARY.md`.
- **TextInputLayout + TextInputEditText** — the password fields. See `docs/GLOSSARY.md`.
- **Material Button** — the "Update password" button. See `docs/GLOSSARY.md`.
- **ProgressBar** — a spinner shown while the request runs. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityResetPasswordBinding`. See `docs/GLOSSARY.md`.

## Screen structure
A vertical `LinearLayout`, top to bottom:

```
LinearLayout (root, vertical, dark background)
├── MaterialToolbar (back arrow)
├── LinearLayout (small hero: logo + "Reset password" title)
└── NestedScrollView
    └── LinearLayout (form)
        ├── subtitle text
        ├── new password field
        ├── confirm new password field
        ├── Update password button
        └── ProgressBar (hidden until loading)
```

The root is a `LinearLayout` because the toolbar, hero, and form are simply stacked in
a column.

## Components

### Toolbar
```xml
<com.google.android.material.appbar.MaterialToolbar
    android:id="@+id/toolbar_reset"
    android:background="@android:color/transparent"
    app:navigationIcon="@drawable/ic_arrow_back_24"
    app:navigationIconTint="@color/brand_white" />
```
- **What it shows:** a transparent bar with a white back arrow over the dark hero.
- **Why it's set up this way:** it is transparent so the dark hero shows through; the
  white arrow tint keeps it visible on the dark background.
- **How the code uses it:** `binding.toolbarReset.setNavigationOnClickListener(...)`
  closes the screen when the back arrow is tapped.

### New password and Confirm fields
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/input_layout_new_password"
    android:hint="@string/hint_new_password"
    app:endIconMode="password_toggle">
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/edit_new_password"
        android:inputType="textPassword" />
</com.google.android.material.textfield.TextInputLayout>

<!-- confirm: id input_layout_confirm_new_password / edit_confirm_new_password -->
```
- **What it shows:** two masked password fields with show/hide eyes.
- **Why it's set up this way:** each `TextInputLayout` wraps its edit text to float the
  hint and show errors; two fields let the app confirm the new password matches before
  sending it.
- **How the code uses it:** `ResetPasswordActivity.attemptReset()` reads
  `binding.editNewPassword` and `binding.editConfirmNewPassword`, checks a minimum
  length and that they match, and shows errors on the matching `inputLayout*`.

### Update button and progress spinner
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_update_password"
    style="@style/Widget.ReClaim.Button.Primary"
    android:text="@string/btn_update_password" />

<ProgressBar
    android:id="@+id/progress"
    android:layout_gravity="center_horizontal"
    android:visibility="gone" />
```
- **What it shows:** the full-width primary button, with a spinner that appears
  underneath while the update is in progress.
- **Why it's set up this way:** the spinner starts with `visibility="gone"` so it takes
  no space until it is needed; the code flips it on during the network call.
- **How the code uses it:** `binding.btnUpdatePassword` runs `attemptReset()`. While
  loading, the code shows `binding.progress`, disables the button and both fields, and
  changes the button text to a "please wait" label.
