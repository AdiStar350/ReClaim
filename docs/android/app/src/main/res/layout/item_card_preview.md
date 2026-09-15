```xml
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/card_item"
    android:layout_width="@dimen/space_200"
    android:layout_height="wrap_content"
    android:clickable="true"
    android:focusable="true"
    app:cardBackgroundColor="@color/md_theme_surfaceContainerLowest"
    app:cardCornerRadius="@dimen/item_card_radius"
    app:cardElevation="@dimen/space_2"
    app:rippleColor="@color/brand_accent"
    app:strokeColor="@color/md_theme_outlineVariant"
    app:strokeWidth="@dimen/space_0_5">
```

What that function does: I am the small tappable card for the home preview row.
What the function is responsible for: I am the root of item_card_preview.
Where it works: PreviewItemAdapter inflates me. HomeFragment hosts that adapter.
Why I chose to write the code of the function the way its written: A fixed 200dp width fits a horizontal list. Clickable so the ripple shows.
More: I use MaterialCardView and item_card_radius.

```xml
        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="@dimen/space_112">

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

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_type"
                style="@style/Widget.ReClaim.Chip.Badge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="bottom|start"
                android:layout_margin="@dimen/space_10"
                android:textColor="@color/brand_white"
                android:textSize="@dimen/text_size_10"
                tools:chipBackgroundColor="@color/type_lost"
                tools:text="Lost" />
        </FrameLayout>
```

What that function does: I show the photo, a dark scrim, and the Lost/Found chip.
What the function is responsible for: I am the image block of the preview card.
Where it works: PreviewItemAdapter.bind loads image_item with Glide and sets chip_type.
Why I chose to write the code of the function the way its written: The scrim makes white chip text readable. 112dp keeps the row short.
More: I use bg_item_image_scrim and Widget.ReClaim.Chip.Badge.

```xml
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingStart="@dimen/space_12"
            android:paddingTop="@dimen/space_10"
            android:paddingEnd="@dimen/space_12"
            android:paddingBottom="@dimen/space_12">

            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_item_title"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:ellipsize="end"
                android:maxLines="1"
                android:textAppearance="?attr/textAppearanceLabelLarge"
                android:textColor="@color/text_primary"
                tools:text="Black Leather Wallet" />

            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_item_location"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_3"
                android:ellipsize="end"
                android:maxLines="1"
                android:textAppearance="?attr/textAppearanceBodySmall"
                android:textColor="@color/text_secondary"
                tools:text="Central Station" />

            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_item_category"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:visibility="gone"
                tools:text="Wallets" />

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_status"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:visibility="gone" />
        </LinearLayout>
```

What that function does: I show title and place. Category and status stay hidden.
What the function is responsible for: I am the text block of the preview card.
Where it works: PreviewItemAdapter.bind sets title and location. The gone views keep ids aligned with item_card.xml.
Why I chose to write the code of the function the way its written: Hidden ids let a shared binding shape exist without showing extra chips on a small card.
More: I use text_item_title and text_item_location.
