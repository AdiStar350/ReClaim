# item_card.xml

## Overview
This layout is one item card — a single row in the search results list: a photo with
Lost/Found and status chips over it, then the title, category, and location. It is
inflated once per item by `ItemAdapter` through `ItemCardBinding` and shown in the
search list (`recycler_items` in `fragment_search.xml`).

Key idea: an **Adapter** inflates this layout once per list item and a **ViewHolder**
holds its views so the list can scroll smoothly. See `docs/GLOSSARY.md`.

## Concepts used here
- **MaterialCardView** — the rounded, tappable card that is the row's root. See `docs/GLOSSARY.md`.
- **LinearLayout** — stacks the image block over the text block, and the text lines. See `docs/GLOSSARY.md`.
- **FrameLayout** — overlays the scrim and chips on top of the photo. See `docs/GLOSSARY.md`.
- **Chip** — the Lost/Found and status badges. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ItemCardBinding`. See `docs/GLOSSARY.md`.

## Screen structure

```
MaterialCardView (@id/card_item — root)
└── LinearLayout (vertical)
    ├── FrameLayout (image area)
    │   ├── ImageView image_item
    │   ├── View (gradient scrim)
    │   └── LinearLayout (bottom): chip_type + Space + chip_status
    └── LinearLayout (text area)
        ├── text_item_title
        ├── text_item_category
        └── LinearLayout: map icon + text_item_location
```

The root is a `MaterialCardView` because each list row is a self-contained, tappable card
with rounded corners and a subtle border.

## Components

### Root card
```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/card_item"
    android:layout_width="match_parent"
    android:clickable="true"
    android:focusable="true"
    app:cardCornerRadius="@dimen/item_card_radius"
    app:rippleColor="@color/brand_accent">
```
- **What it shows:** the whole card container for one item.
- **Why it's set up this way:** `clickable`/`focusable` plus `rippleColor` make the whole
  card a tap target with a ripple; the corner radius and stroke give the card look.
- **How the code uses it:** `ItemAdapter` sets a click listener on the card's root
  (`binding.getRoot().setOnClickListener(...)`) to open the item's details.

### Image with chips
```xml
<FrameLayout android:layout_height="@dimen/space_172">
    <ImageView android:id="@+id/image_item" android:scaleType="centerCrop" ... />
    <View android:background="@drawable/bg_item_image_scrim" />
    <LinearLayout android:layout_gravity="bottom" android:orientation="horizontal">
        <com.google.android.material.chip.Chip android:id="@+id/chip_type" ... />
        <Space android:layout_weight="1" ... />
        <com.google.android.material.chip.Chip android:id="@+id/chip_status" ... />
    </LinearLayout>
</FrameLayout>
```
- **What it shows:** the item photo, darkened by a gradient at the bottom, with a
  Lost/Found chip on the left and a status (Available/Claimed) chip on the right.
- **Why it's set up this way:** a `FrameLayout` stacks the scrim and chips over the
  image; the scrim keeps the white chip text readable on bright photos. A `Space` with
  `layout_weight="1"` between the two chips pushes them to opposite ends.
- **How the code uses it:** the adapter loads the photo into `binding.imageItem` (Glide),
  and sets `binding.chipType` (Lost/Found, color, text) and `binding.chipStatus`
  (Open/Closed, color, text) from the item.

### Text block
```xml
<LinearLayout android:orientation="vertical">
    <com.google.android.material.textview.MaterialTextView android:id="@+id/text_item_title" android:maxLines="1" android:ellipsize="end" ... />
    <com.google.android.material.textview.MaterialTextView android:id="@+id/text_item_category" ... />
    <LinearLayout android:orientation="horizontal">
        <ImageView android:src="@android:drawable/ic_dialog_map" ... />
        <com.google.android.material.textview.MaterialTextView android:id="@+id/text_item_location" android:layout_weight="1" ... />
    </LinearLayout>
</LinearLayout>
```
- **What it shows:** the item title (one line), the category in accent color, and a
  location row with a small map icon.
- **Why it's set up this way:** `maxLines="1"` + `ellipsize="end"` keep long titles and
  locations from breaking the card height; the location text uses `layout_weight="1"` so
  it fills the row next to the icon.
- **How the code uses it:** the adapter sets `binding.textItemTitle`,
  `binding.textItemCategory`, and `binding.textItemLocation` from the item data.
