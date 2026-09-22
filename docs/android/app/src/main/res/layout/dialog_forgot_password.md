# dialog_forgot_password.xml

## Overview
This layout is the custom body of the forgot-password dialog: a short message and one
email field. It is inflated by `LoginActivity.showForgotPasswordDialog()` with
`LayoutInflater.inflate(R.layout.dialog_forgot_password, null)` and set as the view on
a `MaterialAlertDialogBuilder`. The dialog title and Cancel / Send buttons live on the
builder, not in this XML.

Key idea: a **TextInputLayout** wraps the **EditText** so the hint and validation
error stay on the same field. See `docs/GLOSSARY.md`.

## Concepts used here
- **LinearLayout** — vertical stack of message and field. See `docs/GLOSSARY.md`.
- **TextInputLayout + TextInputEditText** — the reset-email field.
- **MaterialAlertDialogBuilder** — hosts this view (in Java).

## Screen structure

```
LinearLayout (root, vertical)
├── MaterialTextView (message — no id)
└── TextInputLayout input_layout_reset_email
    └── TextInputEditText edit_reset_email
```

The root is a vertical `LinearLayout` because the dialog is only a message over one
field.

## Components

### Message
```xml
<com.google.android.material.textview.MaterialTextView
    android:text="@string/forgot_password_dialog_message"
    android:textAppearance="?attr/textAppearanceBodyMedium"
    android:textColor="@color/text_secondary" />
```
- **What it shows:** a short "enter your email to get a reset code" explanation.
- **Why it's set up this way:** body medium and secondary color keep it quieter than
  the dialog title; no id is needed because the text is fixed.
- **How the code uses it:** unused in Java — display only.

### Reset email field
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/input_layout_reset_email"
    style="@style/Widget.ReClaim.TextInput"
    android:hint="@string/hint_email"
    app:startIconDrawable="@android:drawable/ic_dialog_email">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/edit_reset_email"
        android:imeOptions="actionDone"
        android:inputType="textEmailAddress" />
</com.google.android.material.textfield.TextInputLayout>
```
- **What it shows:** the email address that will receive the password-reset OTP.
- **Why it's set up this way:** layout + edit text are one field so errors show under
  the hint. `textEmailAddress` brings the email keyboard; the start icon marks it as
  email; `actionDone` fits a single-field dialog.
- **How the code uses it:** `LoginActivity` finds both ids after inflate, optionally
  pre-fills from the login email field, and on Send validates the address (setting an
  error on the layout if bad) before calling `sendPasswordResetOtp(...)`.
