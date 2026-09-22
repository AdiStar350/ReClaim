# item_my_report.xml

## Overview
This layout is one "my report" row: a left accent bar, thumbnail, title, Lost/Found
chip, category, location, and Edit / Delete text buttons. It is inflated once per
item by `MyReportAdapter` through `ItemMyReportBinding` and shown in
`MyItemsFragment`'s RecyclerView.

Key idea: an **Adapter** inflates this layout once per list item and a **ViewHolder**
holds its views. See `docs/GLOSSARY.md`.

## Concepts used here
- **MaterialCardView** — the rounded card that is the row's root.
- **Chip** — the Lost/Found badge.
- **LinearLayout** — horizontal card content and vertical text stack. See `docs/GLOSSARY.md`.
- **Glide** — loads the report photo. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ItemMyReportBinding`. See `docs/GLOSSARY.md`.

## Screen structure

```
MaterialCardView (root)
└── LinearLayout (horizontal)
    ├── View (accent bar — no id)
    └── LinearLayout (padded row)
        ├── MaterialCardView → ImageView image_thumbnail
        └── LinearLayout (info, weight 1)
            ├── LinearLayout: text_title + chip_type
            ├── text_category
            ├── text_location
            └── LinearLayout: btn_edit + btn_delete
```

The root is a `MaterialCardView` because each of the user's reports is a card with
corners, elevation, and a stroke.

## Components

### Thumbnail and title row
```xml
<ImageView android:id="@+id/image_thumbnail" android:scaleType="centerCrop" ... />
<MaterialTextView android:id="@+id/text_title" android:maxLines="1" android:ellipsize="end" ... />
<com.google.android.material.chip.Chip
    android:id="@+id/chip_type"
    style="@style/Widget.ReClaim.Chip.Badge" ... />
```
- **What it shows:** the report photo, a one-line title, and a Lost/Found chip.
- **Why it's set up this way:** `maxLines="1"` + `ellipsize` keep the row height steady;
  the chip sits beside the title so type is visible at a glance.
- **How the code uses it:** the adapter loads Glide into `binding.imageThumbnail`, sets
  `binding.textTitle`, and styles `binding.chipType` (Lost vs Found colors/text).

### Category and location
```xml
<MaterialTextView android:id="@+id/text_category" android:textColor="@color/brand_accent" ... />
<MaterialTextView android:id="@+id/text_location" android:maxLines="1" android:ellipsize="end" ... />
```
- **What it shows:** the category in accent color and a one-line location.
- **Why it's set up this way:** accent on category matches other item cards; ellipsis
  keeps long addresses from wrapping.
- **How the code uses it:** the adapter sets `binding.textCategory` and
  `binding.textLocation` from the `Item`.

### Edit and Delete
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_edit"
    style="@style/Widget.Material3.Button.TextButton"
    android:text="@string/btn_edit" />

<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_delete"
    style="@style/Widget.Material3.Button.TextButton"
    android:text="@string/btn_delete"
    android:textColor="@color/type_lost" />
```
- **What it shows:** text-style Edit (accent) and Delete (lost-red) actions aligned to
  the end of the row.
- **Why it's set up this way:** text buttons stay compact in a list row; Delete uses a
  warning color. The card itself is not meant to be a tap target.
- **How the code uses it:** the adapter sets listeners on `binding.btnEdit` /
  `binding.btnDelete` to call `Listener.onEdit` / `onDelete`, and clears the root
  click listener so a stray tap on the card does not open edit.
