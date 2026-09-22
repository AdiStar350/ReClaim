# item_card_preview.xml

## Overview
This layout is a compact horizontal preview card for the home screen: a fixed-width
card with a photo, a Lost/Found chip over a scrim, then title and location. It is
inflated once per item by `PreviewItemAdapter` through `ItemCardPreviewBinding` and
shown in `HomeFragment`'s nearby-items RecyclerView. Hidden `text_item_category` and
`chip_status` keep the same ids as `item_card.xml` for adapter compatibility, but stay
`gone`.

Key idea: an **Adapter** inflates this layout once per list item and a **ViewHolder**
holds its views. See `docs/GLOSSARY.md`.

## Concepts used here
- **MaterialCardView** — the tappable preview card root.
- **FrameLayout** — overlays scrim and chip on the photo. See `docs/GLOSSARY.md`.
- **Chip** — the Lost/Found badge.
- **Glide** — loads the preview photo. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ItemCardPreviewBinding`. See `docs/GLOSSARY.md`.

## Screen structure

```
MaterialCardView card_item (fixed width)
└── LinearLayout (vertical)
    ├── FrameLayout (image area)
    │   ├── ImageView image_item
    │   ├── View (scrim — no id)
    │   └── Chip chip_type
    └── LinearLayout (text)
        ├── text_item_title
        ├── text_item_location
        ├── text_item_category (gone)
        └── chip_status (gone)
```

The root is a fixed-width `MaterialCardView` so several cards can sit in a horizontal
preview row.

## Components

### Root card
```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/card_item"
    android:layout_width="@dimen/space_200"
    android:clickable="true"
    android:focusable="true"
    app:rippleColor="@color/brand_accent">
```
- **What it shows:** the whole tappable preview card.
- **Why it's set up this way:** a fixed width fits a horizontal list; `clickable` /
  `focusable` plus ripple make the whole card the tap target.
- **How the code uses it:** `PreviewItemAdapter` sets a click listener on
  `binding.getRoot()` to open the item details.

### Image with type chip
```xml
<FrameLayout android:layout_height="@dimen/space_112">
    <ImageView android:id="@+id/image_item" android:scaleType="centerCrop" ... />
    <View android:background="@drawable/bg_item_image_scrim" />
    <com.google.android.material.chip.Chip
        android:id="@+id/chip_type"
        style="@style/Widget.ReClaim.Chip.Badge"
        android:layout_gravity="bottom|start" ... />
</FrameLayout>
```
- **What it shows:** the item photo, darkened by a bottom scrim, with a Lost/Found
  chip in the corner.
- **Why it's set up this way:** a `FrameLayout` stacks scrim and chip over the image;
  the scrim keeps white chip text readable on bright photos. The image height is
  shorter than full `item_card` so the home row stays compact.
- **How the code uses it:** the adapter loads Glide into `binding.imageItem` and styles
  `binding.chipType` for Lost vs Found.

### Title and location
```xml
<MaterialTextView android:id="@+id/text_item_title" android:maxLines="1" android:ellipsize="end" ... />
<MaterialTextView android:id="@+id/text_item_location" android:maxLines="1" android:ellipsize="end" ... />
```
- **What it shows:** a one-line title and a quieter one-line location under the photo.
- **Why it's set up this way:** single-line ellipsis keeps every preview card the same
  height in the horizontal list.
- **How the code uses it:** the adapter sets `binding.textItemTitle` and
  `binding.textItemLocation` from the `Item`. Category and status views stay unused
  (left `gone` in XML).
