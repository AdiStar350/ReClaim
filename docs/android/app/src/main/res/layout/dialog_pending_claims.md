# dialog_pending_claims.xml

## Overview
This layout is the body of the "pending claims" bottom sheet: a drag handle, a header
with the claim count, and a scrollable list of claim cards. It is inflated by
`PendingClaimsSheet` through `DialogPendingClaimsBinding` and shown from
`HomeFragment` when the user opens the pending-claims card. Each row in the list comes
from `item_pending_claim.xml` via `PendingClaimAdapter`.

Key idea: a **BottomSheetDialog** hosts this layout as its content view; a
**RecyclerView** recycles the claim rows. See `docs/GLOSSARY.md`.

## Concepts used here
- **LinearLayout** — vertical stack (handle, header, list). See `docs/GLOSSARY.md`.
- **RecyclerView** — the scrollable list of pending claims. See `docs/GLOSSARY.md`.
- **Adapter + ViewHolder** — `PendingClaimAdapter` feeds each row. See `docs/GLOSSARY.md`.
- **ViewBinding** — `DialogPendingClaimsBinding`. See `docs/GLOSSARY.md`.

## Screen structure

```
LinearLayout (root, vertical)
├── View (drag handle — no id)
├── MaterialTextView text_pending_claims_header
└── RecyclerView recycler_pending_claims
```

The root is a vertical `LinearLayout` because the sheet is a simple stack: handle,
title, then list.

## Components

### Drag handle
```xml
<View
    android:layout_width="@dimen/space_40"
    android:layout_height="@dimen/space_4"
    android:layout_gravity="center_horizontal"
    android:layout_marginTop="@dimen/space_10"
    android:background="@color/md_theme_outlineVariant" />
```
- **What it shows:** a short centered bar at the top of the sheet (the usual bottom-sheet
  "grab" cue).
- **Why it's set up this way:** it has no id because the code never touches it; it is
  only visual. A thin outline-colored bar matches Material bottom-sheet style.
- **How the code uses it:** unused in Java — display only.

### Header title
```xml
<com.google.android.material.textview.MaterialTextView
    android:id="@+id/text_pending_claims_header"
    android:textAppearance="@style/TextAppearance.ReClaim.SectionTitle"
    android:textColor="@color/brand_dark"
    tools:text="Review Pending Claims (2)" />
```
- **What it shows:** the sheet title, including how many claims are waiting.
- **Why it's set up this way:** section-title styling and side padding keep it readable
  above the list; `tools:text` is design-time only.
- **How the code uses it:** `PendingClaimsSheet.updateHeader()` sets
  `binding.textPendingClaimsHeader` with
  `R.string.pending_claims_sheet_title` and the adapter's item count.

### Pending claims list
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recycler_pending_claims"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:clipToPadding="false"
    android:paddingStart="@dimen/space_20"
    android:paddingEnd="@dimen/space_20"
    android:paddingBottom="@dimen/space_8" />
```
- **What it shows:** the vertical list of claim cards to approve or reject.
- **Why it's set up this way:** side padding frames the cards; `clipToPadding="false"`
  lets rows scroll into the padding without looking clipped.
- **How the code uses it:** `PendingClaimsSheet` sets a `LinearLayoutManager` and
  `PendingClaimAdapter` on `binding.recyclerPendingClaims`, enables nested scrolling,
  and sizes the list height to about 55% of the screen so the sheet is usable.
