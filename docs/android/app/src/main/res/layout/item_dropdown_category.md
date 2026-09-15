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

What that function does: I am one category row with dark text on a light popup.
What the function is responsible for: I am the category dropdown row on the report form.
Where it works: ReportActivity inflates me as R.layout.item_dropdown_category for the category AutoComplete.
Why I chose to write the code of the function the way its written: One line is enough for a category name. Same colors as the places row so both menus look the same.
More: I use textAppearanceBodyLarge and brand_dark.
