```xml
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/card_item"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="@dimen/space_14"
    android:clickable="true"
    android:focusable="true"
    app:cardBackgroundColor="@color/md_theme_surfaceContainerLowest"
    app:cardCornerRadius="@dimen/item_card_radius"
    app:cardElevation="@dimen/space_2"
    app:rippleColor="@color/brand_accent"
    app:strokeColor="@color/md_theme_outlineVariant"
    app:strokeWidth="@dimen/space_0_5">
```

What that function does: I am one lost/found card: photo, chips, title, category, place.
What the function is responsible for: I am the root of item_card.
Where it works: ItemAdapter inflates me. SearchFragment and HomeFragment matches use that adapter. A tap opens ItemDetailsActivity.
Why I chose to write the code of the function the way its written: Clickable card with a ripple is the whole row.
More: I use Widget.ReClaim card colors.

```xml
        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="@dimen/space_172">

            <ImageView
                android:id="@+id/image_item"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:background="@color/md_theme_surfaceContainer"
                android:contentDescription="@string/item_details_title"
                android:scaleType="centerCrop"
                android:src="@drawable/ic_item_placeholder" />

            <View
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:background="@drawable/bg_item_image_scrim" />
```

What that function does: I am the photo with a dark scrim so the chips stay readable.
What the function is responsible for: I show the item image.
Where it works: ItemAdapter loads image_item with Glide.
Why I chose to write the code of the function the way its written: centerCrop fills the 172dp band. Placeholder shows until the URL loads.
More: I use bg_item_image_scrim.

```xml
                <com.google.android.material.chip.Chip
                    android:id="@+id/chip_type"
                    style="@style/Widget.ReClaim.Chip.Badge"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textColor="@color/brand_white"
                    android:textSize="@dimen/text_size_11"
                    tools:chipBackgroundColor="@color/type_lost_text"
                    tools:text="Lost" />

                <com.google.android.material.chip.Chip
                    android:id="@+id/chip_status"
                    style="@style/Widget.ReClaim.Chip.Badge"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textColor="@color/text_primary"
                    android:textSize="@dimen/text_size_11"
                    tools:chipBackgroundColor="@color/status_open_bg"
                    tools:text="Available" />
```

What that function does: I show lost/found on the left and open/closed on the right.
What the function is responsible for: I am the photo badges.
Where it works: ItemAdapter sets chip_type and chip_status.
Why I chose to write the code of the function the way its written: A Space between them pushes status to the end.
More: I use Widget.ReClaim.Chip.Badge.

```xml
            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_item_title"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:ellipsize="end"
                android:maxLines="1"
                android:textAppearance="?attr/textAppearanceTitleMedium"
                android:textColor="@color/text_primary"
                tools:text="Black Leather Wallet" />

            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_item_category"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_3"
                android:ellipsize="end"
                android:maxLines="1"
                android:textAppearance="?attr/textAppearanceLabelLarge"
                android:textColor="@color/brand_accent"
                tools:text="Wallets" />
```

What that function does: I show title and category under the photo.
What the function is responsible for: I am the card text.
Where it works: ItemAdapter sets text_item_title and text_item_category.
Why I chose to write the code of the function the way its written: One line each keeps the list dense. Accent color marks category.
More: I use ellipsize end.

```xml
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_10"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <ImageView
                    android:layout_width="@dimen/space_14"
                    android:layout_height="@dimen/space_14"
                    android:contentDescription="@string/label_location"
                    android:src="@android:drawable/ic_dialog_map"
                    app:tint="@color/text_secondary" />

                <com.google.android.material.textview.MaterialTextView
                    android:id="@+id/text_item_location"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="@dimen/space_6"
                    android:layout_weight="1"
                    android:ellipsize="end"
                    android:maxLines="1"
                    android:textAppearance="?attr/textAppearanceBodySmall"
                    android:textColor="@color/text_secondary"
                    tools:text="Central Bus Station, Tel Aviv" />
            </LinearLayout>
```

What that function does: I show the place with a small map icon.
What the function is responsible for: I am the location line.
Where it works: ItemAdapter sets text_item_location.
Why I chose to write the code of the function the way its written: Secondary color keeps this quieter than the title.
More: I use ic_dialog_map.
