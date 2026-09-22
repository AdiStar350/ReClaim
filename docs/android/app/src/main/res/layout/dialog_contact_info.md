# dialog_contact_info.xml

## Overview
This layout is the body of the contact-info dialog: another person's name, email, and
phone, with copy buttons next to email and phone. It is inflated by
`ContactInfoDialog` through `DialogContactInfoBinding` and shown as the custom view of
a `MaterialAlertDialogBuilder`. Callers include `PendingClaimsSheet`,
`ItemDetailsActivity`, `ReportHistoryActivity`, and `HomeFragment` (each passes a
`ContactResponse` and a title).

Key idea: **ViewBinding** gives typed access to the name/email/phone views and the
copy buttons. See `docs/GLOSSARY.md`.

## Concepts used here
- **LinearLayout** — vertical stack of label/value pairs; horizontal rows for
  value + copy. See `docs/GLOSSARY.md`.
- **ViewBinding** — `DialogContactInfoBinding`. See `docs/GLOSSARY.md`.
- **MaterialAlertDialogBuilder** — hosts this view (in Java).

## Screen structure

```
LinearLayout (root, vertical)
├── MaterialTextView (name label — no id)
├── MaterialTextView text_contact_name
├── MaterialTextView (email label — no id)
├── LinearLayout (horizontal)
│   ├── MaterialTextView text_contact_email
│   └── ImageButton btn_copy_email
├── MaterialTextView (phone label — no id)
└── LinearLayout (horizontal)
    ├── MaterialTextView text_contact_phone
    └── ImageButton btn_copy_phone
```

The root is a vertical `LinearLayout` because the dialog is stacked label/value pairs.

## Components

### Name
```xml
<com.google.android.material.textview.MaterialTextView
    android:text="@string/contact_info_label_name"
    android:textAppearance="?attr/textAppearanceLabelMedium"
    android:textColor="@color/text_secondary" />

<com.google.android.material.textview.MaterialTextView
    android:id="@+id/text_contact_name"
    android:textAppearance="?attr/textAppearanceBodyLarge"
    android:textColor="@color/brand_dark" />
```
- **What it shows:** a "Name" label and the contact's display name.
- **Why it's set up this way:** secondary label + dark body text matches the email and
  phone blocks so the dialog reads consistently.
- **How the code uses it:** `ContactInfoDialog.bindContact()` sets
  `binding.textContactName` from `ContactResponse.getName()` (or an em dash if empty).

### Email with copy
```xml
<LinearLayout android:orientation="horizontal" android:gravity="center_vertical">
    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/text_contact_email"
        android:layout_width="0dp"
        android:layout_weight="1"
        android:textIsSelectable="true" />
    <ImageButton
        android:id="@+id/btn_copy_email"
        android:src="@drawable/ic_copy"
        android:contentDescription="@string/cd_copy_email" />
</LinearLayout>
```
- **What it shows:** the email address and a small copy button beside it.
- **Why it's set up this way:** `layout_weight="1"` lets the text fill the row;
  `textIsSelectable` lets the user select without using the button; the borderless
  ripple keeps the icon light.
- **How the code uses it:** `bindContact()` sets `binding.textContactEmail` and wires
  `binding.btnCopyEmail` to copy the email to the clipboard (hides the button if email
  is empty).

### Phone with copy
```xml
<LinearLayout android:orientation="horizontal" android:gravity="center_vertical">
    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/text_contact_phone"
        android:layout_width="0dp"
        android:layout_weight="1"
        android:textIsSelectable="true" />
    <ImageButton
        android:id="@+id/btn_copy_phone"
        android:src="@drawable/ic_copy"
        android:contentDescription="@string/cd_copy_phone" />
</LinearLayout>
```
- **What it shows:** the phone number and a copy button beside it.
- **Why it's set up this way:** same pattern as email so both contact channels work the
  same way.
- **How the code uses it:** `bindContact()` sets `binding.textContactPhone` and wires
  `binding.btnCopyPhone` the same way as email (hides the button if phone is empty).
