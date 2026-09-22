# activity_login.xml

## Overview
This layout is the login screen: a dark "hero" banner (logo, app name, subtitle) on
top and a light form sheet with email/password fields below. It is inflated by
`LoginActivity` through the generated binding `ActivityLoginBinding`
(`binding = ActivityLoginBinding.inflate(...)` then `setContentView(binding.getRoot())`).

Key idea: an **Activity** is one full screen, and the **layout XML** describes what
that screen looks like. See `docs/GLOSSARY.md`.

## Concepts used here
- **LinearLayout** — a container that stacks its children in one direction (here,
  vertically). See `docs/GLOSSARY.md`.
- **NestedScrollView** — a scrolling container so the form still fits on small
  screens. See `docs/GLOSSARY.md`.
- **TextInputLayout + TextInputEditText** — a Material text field: the outer layout
  draws the hint, box, and error text; the inner edit text is where you type. See
  `docs/GLOSSARY.md`.
- **Material Button** — the styled buttons (login, forgot password, register). See
  `docs/GLOSSARY.md`.
- **ViewBinding** — the generated `ActivityLoginBinding` gives typed access to the
  views by id, so no `findViewById`. See `docs/GLOSSARY.md`.

## Screen structure
The whole screen is a vertical `LinearLayout`. It has two parts stacked top to bottom:

```
LinearLayout (root, vertical, dark background)
├── LinearLayout  (hero: logo + title + subtitle, fixed height)
└── NestedScrollView  (light rounded form sheet, fills the rest)
    └── LinearLayout (vertical: title, email, password, buttons, "or", register row)
```

The root is a `LinearLayout` because the design is just two blocks in a column: a
fixed-height hero and a form that takes whatever height is left. That is simpler than
a `ConstraintLayout` here.

## Components

### Root and hero banner
```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/brand_deep"
    android:fitsSystemWindows="true"
    android:orientation="vertical">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="@dimen/auth_hero_height"
        android:gravity="center"
        android:orientation="vertical">

        <ImageView android:id="@+id/image_logo" ... android:src="@mipmap/ic_launcher" />
        <com.google.android.material.textview.MaterialTextView android:id="@+id/text_login_title" ... />
        <com.google.android.material.textview.MaterialTextView android:id="@+id/text_login_subtitle" ... />
    </LinearLayout>
```
- **What it shows:** the dark brand area at the top with the app logo, the app name,
  and a short subtitle.
- **Why it's set up this way:** `fitsSystemWindows="true"` keeps the content below the
  status bar so the clock/battery don't overlap the logo. The hero has a fixed height
  (`auth_hero_height`) so login and register look the same. Centering (`gravity="center"`)
  stacks the three items neatly in the middle.
- **How the code uses it:** these ids (`image_logo`, `text_login_title`,
  `text_login_subtitle`) are static; `LoginActivity` does not read or change them.

### Form sheet container
```xml
<androidx.core.widget.NestedScrollView
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1"
    android:background="@drawable/bg_auth_form"
    android:fillViewport="true">
```
- **What it shows:** the light, rounded sheet that holds the form and scrolls if the
  keyboard covers part of it.
- **Why it's set up this way:** `height="0dp"` plus `layout_weight="1"` means "take all
  the space left under the hero." `fillViewport="true"` lets the inner content stretch
  to fill the view even when it is short. `bg_auth_form` gives the rounded top edge.
- **How the code uses it:** it is a passive container; the Activity never touches it.

### Email field
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/input_layout_email"
    style="@style/Widget.ReClaim.TextInput"
    android:hint="@string/hint_email"
    app:startIconDrawable="@android:drawable/ic_dialog_email">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/edit_email"
        android:inputType="textEmailAddress" />
</com.google.android.material.textfield.TextInputLayout>
```
- **What it shows:** the email input, with an envelope icon and the hint "Email".
- **Why it's set up this way:** the `TextInputLayout` wraps the edit text so it can draw
  the floating hint and show a red error message under the box; that is why error text
  is set on the layout, not the edit text. `inputType="textEmailAddress"` brings up the
  email keyboard (with the `@` key).
- **How the code uses it:** `LoginActivity` reads `binding.editEmail.getText()` in
  `attemptLogin()`, and shows problems with `binding.inputLayoutEmail.setError(...)`.

### Password field
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/input_layout_password"
    style="@style/Widget.ReClaim.TextInput"
    android:hint="@string/hint_password"
    app:endIconMode="password_toggle">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/edit_password"
        android:inputType="textPassword" />
</com.google.android.material.textfield.TextInputLayout>
```
- **What it shows:** the password input with a "show/hide" eye icon on the right.
- **Why it's set up this way:** `inputType="textPassword"` masks the characters, and
  `endIconMode="password_toggle"` adds the eye so the user can check what they typed.
- **How the code uses it:** `LoginActivity` reads `binding.editPassword.getText()` and
  reports errors via `binding.inputLayoutPassword.setError(...)`.

### Forgot-password and Login buttons
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_forgot_password"
    style="@style/Widget.Material3.Button.TextButton"
    android:layout_gravity="end"
    android:text="@string/btn_forgot_password" />

<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_login"
    style="@style/Widget.ReClaim.Button.Primary"
    android:layout_width="match_parent"
    android:text="@string/btn_login" />
```
- **What it shows:** a small "Forgot password?" text button aligned to the right, then
  the full-width primary "Log in" button.
- **Why it's set up this way:** the forgot-password action is secondary, so it uses a
  quiet text-button style and sits to the side (`layout_gravity="end"`). The main login
  action uses the filled primary style and full width so it is the obvious next tap.
- **How the code uses it:** `binding.btnLogin` runs `attemptLogin()`;
  `binding.btnForgotPassword` opens the forgot-password dialog (see
  `dialog_forgot_password.xml`). Both are disabled while a login request is in flight.

### "Register instead" row
```xml
<LinearLayout
    android:orientation="horizontal"
    android:gravity="center">

    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/text_no_account"
        android:text="@string/login_no_account" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_go_register"
        style="@style/Widget.Material3.Button.TextButton"
        android:text="@string/btn_go_register" />
</LinearLayout>
```
- **What it shows:** a centered row with the text "Don't have an account?" next to a
  "Register" text button. (Just above it, an "or" divider row uses two thin `View`
  lines with a label in the middle.)
- **Why it's set up this way:** a horizontal `LinearLayout` keeps the label and button
  on one line; a text button (not a second filled button) keeps this quieter than the
  primary "Log in" button.
- **How the code uses it:** `binding.btnGoRegister` opens `RegisterActivity`. The label
  `text_no_account` is static.
