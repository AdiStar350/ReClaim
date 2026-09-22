# dialog_delete_account.xml

## Overview
This layout is the custom body of the "delete account" confirmation dialog: a short
warning message plus name and password fields the user must fill before the account
is removed. It is inflated by `ProfileSettingsFragment.showDeleteAccountDialog()` with
`LayoutInflater.inflate(R.layout.dialog_delete_account, null)` and set as the view on a
`MaterialAlertDialogBuilder`. The dialog title and Cancel / Delete buttons live on the
builder, not in this XML.

Key idea: a **TextInputLayout** wraps each **EditText** so hints and errors stay with
the field. See `docs/GLOSSARY.md`.

## Concepts used here
- **LinearLayout** — vertical stack of message and two fields. See `docs/GLOSSARY.md`.
- **TextInputLayout + TextInputEditText** — name and password confirmation fields.
- **MaterialAlertDialogBuilder** — hosts this view (in Java).

## Screen structure

```
LinearLayout (root, vertical)
├── MaterialTextView (warning message — no id)
├── TextInputLayout input_layout_confirm_name
│   └── TextInputEditText edit_confirm_name
└── TextInputLayout input_layout_confirm_password
    └── TextInputEditText edit_confirm_password
```

The root is a vertical `LinearLayout` because the dialog is just a message over two
fields.

## Components

### Warning message
```xml
<com.google.android.material.textview.MaterialTextView
    android:text="@string/delete_account_dialog_message"
    android:textColor="@color/brand_slate" />
```
- **What it shows:** the explanation that deleting the account is permanent and that
  name + password are required.
- **Why it's set up this way:** no id is needed — the text is fixed from strings.
  Slate color keeps it quieter than the dialog title.
- **How the code uses it:** unused in Java — display only.

### Confirm name field
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/input_layout_confirm_name"
    style="@style/Widget.ReClaim.TextInput"
    android:hint="@string/hint_confirm_name">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/edit_confirm_name"
        android:inputType="textPersonName" />
</com.google.android.material.textfield.TextInputLayout>
```
- **What it shows:** a field where the user retypes their name to confirm deletion.
- **Why it's set up this way:** layout + edit text are one field so the fragment can
  show a validation error on the layout. `textPersonName` brings a name-friendly
  keyboard.
- **How the code uses it:** `ProfileSettingsFragment` finds both ids after inflate;
  on Delete it reads `edit_confirm_name`, and if empty sets an error on
  `input_layout_confirm_name`.

### Confirm password field
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/input_layout_confirm_password"
    style="@style/Widget.ReClaim.TextInput"
    android:hint="@string/hint_password"
    app:endIconMode="password_toggle">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/edit_confirm_password"
        android:inputType="textPassword" />
</com.google.android.material.textfield.TextInputLayout>
```
- **What it shows:** a password field with a show/hide toggle, so the user proves they
  own the account.
- **Why it's set up this way:** `password_toggle` and `textPassword` are the standard
  Material password pattern; the layout holds empty-password errors.
- **How the code uses it:** the fragment finds both ids; on Delete it reads the
  password, and if empty sets an error on `input_layout_confirm_password`. If both
  fields are filled it calls `deleteAccount(name, password)`.
