# item_places_dropdown.xml

## Overview
This layout is one row in the Google Places address suggestions list on the report
screen: a single `TextView` with dark text on a light background (up to two lines).
It is used by `PlacesAutocompleteAdapter`, which extends `ArrayAdapter` with
`R.layout.item_places_dropdown` as both the item and drop-down resource.
`ReportActivity` creates that adapter with a `PlacesClient` and binds it to the
location field.

Key idea: **Google Places** autocomplete predictions are shown as plain text rows;
this layout is the row look. See `docs/GLOSSARY.md`.

## Concepts used here
- **TextView** — one address suggestion per row.
- **ArrayAdapter** — `PlacesAutocompleteAdapter` fills the dropdown.
- **Google Places / Maps** — predictions come from the Places API. See `docs/GLOSSARY.md`.

## Screen structure

```
TextView (root — no id; adapter sets the text)
```

A single `TextView` is enough because each suggestion is just the place's full text.

## Components

### Place suggestion row
```xml
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/md_theme_surfaceContainerLowest"
    android:ellipsize="end"
    android:maxLines="2"
    android:paddingStart="@dimen/space_16"
    android:paddingTop="@dimen/space_14"
    android:paddingEnd="@dimen/space_16"
    android:paddingBottom="@dimen/space_14"
    android:textAppearance="?attr/textAppearanceBodyLarge"
    android:textColor="@color/brand_dark" />
```
- **What it shows:** one address/place prediction (up to two lines) in the autocomplete
  popup.
- **Why it's set up this way:** light surface + dark text keep long addresses readable;
  `maxLines="2"` fits fuller place names better than the single-line category row. No
  id is needed because the adapter writes into the root `TextView`.
- **How the code uses it:** `PlacesAutocompleteAdapter` passes this layout to
  `ArrayAdapter`'s constructor and `setDropDownViewResource`. `getView` /
  `getDropDownView` call `super` (which inflates this layout) then
  `styleDropdownRow` to reinforce background and text color. The row text is the
  prediction's `getFullText(...)`.
