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

What that function does: I am one address suggestion row with dark text on a light background.
What the function is responsible for: I am the Places dropdown row.
Where it works: PlacesAutocompleteAdapter uses me as the item layout and drop-down resource. ReportActivity hosts that adapter.
Why I chose to write the code of the function the way its written: Two lines and ellipsize keep long addresses readable. brand_dark stays readable on the light popup.
More: I use textAppearanceBodyLarge and md_theme_surfaceContainerLowest.
