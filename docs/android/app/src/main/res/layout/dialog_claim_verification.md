# dialog_claim_verification.xml

## Overview
This layout is the custom body of the "submit a claim" dialog: one multi-line answer
field (with a character counter) where the user answers the item owner's verification
question. It is inflated by `ItemDetailsActivity.showClaimDialog()` with
`LayoutInflater.inflate(R.layout.dialog_claim_verification, null)` and attached as the
dialog's view. The dialog title and question text come from
`MaterialAlertDialogBuilder`, not from this file.

Key idea: a **TextInputLayout** wraps the **EditText** so the hint, error, and counter
live on the same field. See `docs/GLOSSARY.md`.

## Concepts used here
- **TextInputLayout + TextInputEditText** — one outlined answer field with a counter.
- **MaterialAlertDialogBuilder** — hosts this view inside an alert dialog (in Java).

## Screen structure

```
TextInputLayout input_layout_claim_answer (root)
└── TextInputEditText edit_claim_answer
```

The root is the `TextInputLayout` itself because the dialog only needs this one field.

## Components

### Claim answer field
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/input_layout_claim_answer"
    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
    android:hint="@string/hint_claim_answer"
    app:counterEnabled="true"
    app:counterMaxLength="500">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/edit_claim_answer"
        android:inputType="textMultiLine"
        android:maxLines="4"
        android:minLines="2" />
</com.google.android.material.textfield.TextInputLayout>
```
- **What it shows:** a multi-line box for the user's verification answer, with a 500-
  character counter under it.
- **Why it's set up this way:** grouping layout + edit text as one field keeps the
  hint, error, and counter together. `textMultiLine` with 2–4 lines fits short answers;
  the counter matches the backend's answer length limit. Side margins give the field
  room inside the alert dialog.
- **How the code uses it:** `ItemDetailsActivity` finds
  `R.id.input_layout_claim_answer` and `R.id.edit_claim_answer` after inflate. On
  Submit it reads the trimmed text; if empty it sets an error on the layout and
  keeps the dialog open; otherwise it dismisses and calls `submitClaimToBackend(answer)`.
