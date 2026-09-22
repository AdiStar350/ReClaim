# dialog_review_incoming_claim.xml

## Overview
This layout is a small dialog body that would show one incoming claim for review: the
item name, the verification question, and the claimant's answer. **It is unused** —
no Activity, Fragment, Adapter, or helper in the project inflates
`R.layout.dialog_review_incoming_claim` or references its view ids. Live claim review
uses `dialog_pending_claims.xml` / `item_pending_claim.xml` (and the full-screen
`activity_pending_claims.xml`) instead.

Key idea: a **Layout XML** can exist in `res/layout` without being wired up; this one
looks like an older or alternate design that was never connected. See `docs/GLOSSARY.md`.

## Concepts used here
- **LinearLayout** — vertical stack of labels and values. See `docs/GLOSSARY.md`.
- **MaterialTextView** — the text widgets for item, question, and answer.

## Screen structure

```
LinearLayout (root, vertical)
├── MaterialTextView text_review_item
├── MaterialTextView (static "question" label — no id)
├── MaterialTextView text_review_question
├── MaterialTextView (static "answer" label — no id)
└── MaterialTextView text_review_answer
```

The root is a vertical `LinearLayout` because the dialog is only stacked text pairs.

## Components

### Item title
```xml
<com.google.android.material.textview.MaterialTextView
    android:id="@+id/text_review_item"
    android:textAppearance="?attr/textAppearanceTitleMedium"
    android:textColor="@color/brand_dark" />
```
- **What it shows:** the name of the item the claim is about (if anything ever set it).
- **Why it's set up this way:** title-medium and brand-dark make it the strongest line
  in the dialog.
- **How the code uses it:** unused — no Java caller sets this id.

### Question block
```xml
<com.google.android.material.textview.MaterialTextView
    android:text="@string/claim_review_question_label"
    android:textAppearance="?attr/textAppearanceLabelMedium"
    android:textColor="@color/text_secondary" />

<com.google.android.material.textview.MaterialTextView
    android:id="@+id/text_review_question"
    android:textAppearance="?attr/textAppearanceBodyLarge"
    android:textColor="@color/brand_dark" />
```
- **What it shows:** a quiet "question" label, then the verification question text.
- **Why it's set up this way:** label + value is a common read-only dialog pattern;
  secondary color on the label keeps the value more prominent.
- **How the code uses it:** unused — no Java caller sets `text_review_question`.

### Answer block
```xml
<com.google.android.material.textview.MaterialTextView
    android:text="@string/claim_review_answer_label"
    android:textAppearance="?attr/textAppearanceLabelMedium"
    android:textColor="@color/text_secondary" />

<com.google.android.material.textview.MaterialTextView
    android:id="@+id/text_review_answer"
    android:textAppearance="?attr/textAppearanceBodyLarge"
    android:textColor="@color/brand_dark" />
```
- **What it shows:** a quiet "answer" label, then the claimant's written answer.
- **Why it's set up this way:** same label/value pairing as the question block so the
  two read the same way.
- **How the code uses it:** unused — no Java caller sets `text_review_answer`.
