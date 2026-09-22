# item_dropdown_category.xml

## Overview
This layout is one row in the category dropdown on the new-report screen: a single
`TextView` with dark text on a light background. It is used by `ReportActivity` in
`setupCategoryDropdown()`, which builds an `ArrayAdapter` with
`R.layout.item_dropdown_category` and the `item_categories` string array, then attaches
it to `binding.dropdownCategory`.

Key idea: an **ArrayAdapter** inflates a simple layout for each dropdown suggestion
row; there is no generated binding class for this tiny file. See `docs/GLOSSARY.md`.

## Concepts used here
- **TextView** — one line of category text per row.
- **ArrayAdapter** — fills the AutoComplete / dropdown from a string array (in Java).

## Screen structure

```
TextView (root — no id; adapter sets the text)
```

A single `TextView` is enough because each category is just a label.

## Components

### Category row
```xml
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/md_theme_surfaceContainerLowest"
    android:ellipsize="end"
    android:maxLines="1"
    android:paddingStart="@dimen/space_16"
    android:paddingTop="@dimen/space_14"
    android:paddingEnd="@dimen/space_16"
    android:paddingBottom="@dimen/space_14"
    android:textAppearance="?attr/textAppearanceBodyLarge"
    android:textColor="@color/brand_dark" />
```
- **What it shows:** one category name (e.g. Wallets) in the popup list.
- **Why it's set up this way:** light surface + dark text keeps suggestions readable
  even when the surrounding screen is darker; padding and one-line ellipsis match a
  Material dropdown row. No id is needed because `ArrayAdapter` writes into the root
  `TextView` itself.
- **How the code uses it:** `ReportActivity.setupCategoryDropdown()` creates
  `new ArrayAdapter<>(this, R.layout.item_dropdown_category, categories)` and calls
  `binding.dropdownCategory.setAdapter(adapter)`. The adapter binds each string into
  this `TextView` when the dropdown opens.
