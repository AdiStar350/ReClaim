# item_history.xml

## Overview
This layout is one report-history row: a thumbnail, a role label ("You reported" /
claim wording), the item title, an optional "View contact" button, and a status badge.
It is inflated once per entry by `HistoryAdapter` through `ItemHistoryBinding` and
shown in `ReportHistoryActivity`'s RecyclerView.

Key idea: an **Adapter** inflates this layout once per list item and a **ViewHolder**
holds its views. See `docs/GLOSSARY.md`.

## Concepts used here
- **MaterialCardView** — the rounded card that is the row's root.
- **LinearLayout** — horizontal row of thumbnail, text, and status. See `docs/GLOSSARY.md`.
- **Glide** — loads the history photo. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ItemHistoryBinding`. See `docs/GLOSSARY.md`.

## Screen structure

```
MaterialCardView (root)
└── LinearLayout (horizontal, padded)
    ├── MaterialCardView → ImageView image_history
    ├── LinearLayout (weight 1, vertical)
    │   ├── text_history_role
    │   ├── text_history_title
    │   └── btn_history_contact (gone by default)
    └── text_history_status (badge)
```

The root is a `MaterialCardView` because each history entry is a compact card.

## Components

### Thumbnail and text
```xml
<ImageView android:id="@+id/image_history" android:scaleType="centerCrop" ... />
<MaterialTextView android:id="@+id/text_history_role" ... />
<MaterialTextView
    android:id="@+id/text_history_title"
    android:maxLines="2"
    android:ellipsize="end" ... />
```
- **What it shows:** a small photo, a role line (reporter vs claimant), and the item
  title (up to two lines).
- **Why it's set up this way:** the nested card rounds the thumbnail; role in secondary
  color sits above the stronger title.
- **How the code uses it:** the adapter loads Glide into `binding.imageHistory`, sets
  `binding.textHistoryRole` from a string resource on the entry, and sets
  `binding.textHistoryTitle` (or a placeholder if empty).

### Contact button
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_history_contact"
    style="@style/Widget.Material3.Button.TextButton"
    android:text="@string/btn_view_contact_info"
    android:visibility="gone" />
```
- **What it shows:** a "View contact info" text button when the user is allowed to see
  the other party's contact (e.g. an approved claim).
- **Why it's set up this way:** default `gone` keeps most rows compact; only approved
  claim rows need the extra action.
- **How the code uses it:** the adapter shows `binding.btnHistoryContact` when
  `entry.showContact` is true and a claim id exists, and calls
  `ContactClickListener.onViewContact(claimId)` (which leads to `ContactInfoDialog` in
  `ReportHistoryActivity`).

### Status badge
```xml
<com.google.android.material.textview.MaterialTextView
    android:id="@+id/text_history_status"
    android:background="@drawable/bg_status_badge"
    android:textStyle="bold" />
```
- **What it shows:** Pending / Approved / Declined on the right side of the row.
- **Why it's set up this way:** a tinted badge background keeps status readable without
  a full Chip widget.
- **How the code uses it:** the adapter sets label and tint colors from
  `HistoryAdapter.Status` on `binding.textHistoryStatus`.
