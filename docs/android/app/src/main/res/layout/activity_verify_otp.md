# activity_verify_otp.xml

## Overview
This layout is the "enter the 6-digit code" screen used during sign-up and during
password reset. It has a back-arrow toolbar, a small hero, one code field, a verify
button, a spinner, and a "resend code" row. It is inflated by `VerifyOtpActivity`
through `ActivityVerifyOtpBinding`.

Key idea: an **OTP** is a one-time code emailed to prove the user owns the address;
this **Activity** collects that code. See `docs/GLOSSARY.md`.

## Concepts used here
- **LinearLayout** — vertical stack for the whole screen. See `docs/GLOSSARY.md`.
- **MaterialToolbar** — top bar with the back arrow. See `docs/GLOSSARY.md`.
- **NestedScrollView** — scrolls the form if needed. See `docs/GLOSSARY.md`.
- **TextInputLayout + TextInputEditText** — the code field. See `docs/GLOSSARY.md`.
- **Material Button** — verify and resend buttons. See `docs/GLOSSARY.md`.
- **ProgressBar** — spinner during the check. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityVerifyOtpBinding`. See `docs/GLOSSARY.md`.

## Screen structure
A vertical `LinearLayout`:

```
LinearLayout (root, vertical, dark background)
├── MaterialToolbar (back arrow)
├── LinearLayout (small hero: logo + "Verify" title)
└── NestedScrollView
    └── LinearLayout (form)
        ├── subtitle text (tells you which email got the code)
        ├── OTP code field
        ├── Verify button
        ├── ProgressBar (hidden)
        └── "No code? Resend" row
```

The root is a `LinearLayout` because everything is stacked in one column.

## Components

### Toolbar
```xml
<com.google.android.material.appbar.MaterialToolbar
    android:id="@+id/toolbar_verify"
    android:background="@android:color/transparent"
    app:navigationIcon="@drawable/ic_arrow_back_24"
    app:navigationIconTint="@color/brand_white" />
```
- **What it shows:** a transparent bar with a white back arrow over the dark hero.
- **Why it's set up this way:** transparent so the hero color shows through; the white
  tint keeps the arrow readable on the dark background.
- **How the code uses it:** `binding.toolbarVerify.setNavigationOnClickListener(...)`
  navigates back.

### Subtitle
```xml
<com.google.android.material.textview.MaterialTextView
    android:id="@+id/text_verify_subtitle"
    android:text="@string/verify_title" />
```
- **What it shows:** a line telling the user a code was sent to their email.
- **Why it's set up this way:** it is a plain text view because the message changes at
  runtime (it includes the email address and differs between sign-up and reset).
- **How the code uses it:** `binding.textVerifySubtitle.setText(getString(subtitleRes, email))`
  fills in the right message and email.

### OTP code field
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/input_layout_otp"
    android:hint="@string/hint_otp_code"
    app:startIconDrawable="@android:drawable/ic_lock_lock">
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/edit_otp"
        android:inputType="number"
        android:letterSpacing="0.3"
        android:maxLength="6"
        android:textSize="@dimen/text_size_20" />
</com.google.android.material.textfield.TextInputLayout>
```
- **What it shows:** a single field for the 6-digit code, with a lock icon, wide letter
  spacing, and large text.
- **Why it's set up this way:** `inputType="number"` shows a number pad, `maxLength="6"`
  stops after six digits, and the larger text plus `letterSpacing` make the code easy to
  read as you type. The `TextInputLayout` shows the error if the code is empty or wrong.
- **How the code uses it:** `VerifyOtpActivity.attemptVerify()` reads
  `binding.editOtp.getText()`, requires it to be non-empty, and shows problems on
  `binding.inputLayoutOtp` (for example "code required" or "wrong code").

### Verify button, spinner, and Resend row
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_verify"
    style="@style/Widget.ReClaim.Button.Primary"
    android:text="@string/btn_verify" />

<ProgressBar android:id="@+id/progress" android:visibility="gone" />

<LinearLayout android:orientation="horizontal" android:gravity="center">
    <com.google.android.material.textview.MaterialTextView android:text="@string/verify_no_code" ... />
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_resend_code"
        style="@style/Widget.Material3.Button.TextButton"
        android:text="@string/btn_resend_code" />
</LinearLayout>
```
- **What it shows:** the primary "Verify" button, a hidden spinner, and a centered
  "Didn't get a code? Resend" row.
- **Why it's set up this way:** the primary button is the main action; resending is
  secondary, so it is a quiet text button. The spinner is `gone` until a request runs.
- **How the code uses it:** `binding.btnVerify` runs `attemptVerify()` and
  `binding.btnResendCode` runs `resendCode()`. While loading, the code shows
  `binding.progress`, disables the buttons and `binding.editOtp`, and sets a "please
  wait" label on the verify button. The verify button's idle text switches between
  "Verify" and "Continue" depending on whether this is sign-up or reset.
