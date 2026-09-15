```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/md_theme_background">
```

What that function does: I am the item details screen: collapsing photo, facts, reporter, and a bottom button.
What the function is responsible for: I am the root of activity_item_details.
Where it works: ItemDetailsActivity inflates me through ActivityItemDetailsBinding.
Why I chose to write the code of the function the way its written: CoordinatorLayout lets the photo collapse while the claim bar stays down.
More: I use CoordinatorLayout.

```xml
    <com.google.android.material.appbar.AppBarLayout
        android:id="@+id/app_bar_layout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:fitsSystemWindows="true"
        app:statusBarForeground="@color/brand_dark">

        <com.google.android.material.appbar.CollapsingToolbarLayout
            android:id="@+id/collapsing_toolbar"
            android:layout_width="match_parent"
            android:layout_height="@dimen/space_292"
            android:fitsSystemWindows="true"
            app:contentScrim="@color/brand_dark"
            app:layout_scrollFlags="scroll|exitUntilCollapsed"
            app:titleCollapseMode="scale"
            app:titleTextColor="@color/brand_white">

            <FrameLayout
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                app:layout_collapseMode="parallax">

                <ImageView
                    android:id="@+id/image_item"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:contentDescription="@string/item_details_title"
                    android:fitsSystemWindows="true"
                    android:scaleType="centerCrop"
                    android:src="@drawable/ic_item_placeholder" />

                <View
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:background="@drawable/bg_item_image_scrim" />
            </FrameLayout>

            <com.google.android.material.appbar.MaterialToolbar
                android:id="@+id/toolbar_item_details"
                android:layout_width="match_parent"
                android:layout_height="?attr/actionBarSize"
                app:layout_collapseMode="pin"
                app:navigationIcon="@drawable/ic_arrow_back_24"
                app:navigationIconTint="@color/brand_white"
                app:titleTextColor="@color/brand_white" />

        </com.google.android.material.appbar.CollapsingToolbarLayout>

    </com.google.android.material.appbar.AppBarLayout>
```

What that function does: I am the collapsing photo with a pinned back arrow.
What the function is responsible for: I am the details app bar.
Where it works: ItemDetailsActivity.setupToolbar uses toolbar_item_details. Glide loads image_item.
Why I chose to write the code of the function the way its written: Title is off in Java so it sits under the photo instead of on it. The scrim keeps the white arrow readable.
More: I use CollapsingToolbarLayout.

```xml
            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_item_title"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textAppearance="?attr/textAppearanceHeadlineMedium"
                android:textColor="@color/brand_dark" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_14"
                android:orientation="horizontal">

                <com.google.android.material.chip.Chip
                    android:id="@+id/chip_status"
                    style="@style/Widget.Material3.Chip.Assist"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/status_open"
                    android:textColor="@color/status_open_text"
                    app:chipBackgroundColor="@color/status_open_bg"
                    app:chipCornerRadius="@dimen/chip_corner" />

                <com.google.android.material.chip.Chip
                    android:id="@+id/chip_category"
                    style="@style/Widget.Material3.Chip.Assist"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="@dimen/space_8"
                    android:textColor="@color/brand_dark"
                    app:chipBackgroundColor="@color/md_theme_surfaceContainerHigh"
                    app:chipCornerRadius="@dimen/chip_corner" />

            </LinearLayout>
```

What that function does: I show the title, open/closed, and category.
What the function is responsible for: I am the details heading.
Where it works: ItemDetailsActivity.bindFields sets these.
Why I chose to write the code of the function the way its written: Title under the photo is large. Chips stay small.
More: NestedScrollView uses appbar_scrolling_view_behavior.

```xml
            <com.google.android.material.card.MaterialCardView
                style="@style/Widget.ReClaim.Card"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_20">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="@dimen/space_18">

                    <com.google.android.material.textview.MaterialTextView
                        android:id="@+id/label_description"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="@string/label_description"
                        android:textAppearance="?attr/textAppearanceTitleMedium"
                        android:textColor="@color/brand_dark" />

                    <com.google.android.material.textview.MaterialTextView
                        android:id="@+id/text_description"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="@dimen/space_10"
                        android:textAppearance="?attr/textAppearanceBodyLarge"
                        android:textColor="@color/brand_slate" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>
```

What that function does: I am the description card.
What the function is responsible for: I show what the item is.
Where it works: ItemDetailsActivity.bindFields sets text_description.
Why I chose to write the code of the function the way its written: A card keeps a long description off the chips.
More: I use Widget.ReClaim.Card.

```xml
            <com.google.android.material.card.MaterialCardView
                style="@style/Widget.ReClaim.Card"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_16">
```

What that function does: I am the place and date card.
What the function is responsible for: I show where and when.
Where it works: ItemDetailsActivity.bindFields sets text_location and text_date_found.
Why I chose to write the code of the function the way its written: Two labeled rows with a divider keep map and calendar together.
More: I use MaterialDivider between the rows.

```xml
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/card_reporter"
                style="@style/Widget.ReClaim.Card"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_12"
                android:clickable="false"
                android:focusable="false">
```

What that function does: I show the reporter name and avatar. I am not tappable.
What the function is responsible for: I am the reported-by card.
Where it works: ItemDetailsActivity.bindFields sets text_reported_by and image_reporter_avatar.
Why I chose to write the code of the function the way its written: clickable false stops a ripple. Contact is not on this card.
More: I use a round avatar card.

```xml
    <FrameLayout
        android:id="@+id/bottom_claim_container"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        android:background="@color/md_theme_surfaceContainerLowest"
        android:elevation="@dimen/space_10"
        android:padding="@dimen/space_16">

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_claim"
            style="@style/Widget.ReClaim.Button.Primary"
            android:layout_width="match_parent"
            android:layout_height="@dimen/space_56"
            android:text="@string/btn_claim"
            android:textAppearance="?attr/textAppearanceTitleMedium" />

    </FrameLayout>
```

What that function does: I am the bottom claim / I-found-this bar.
What the function is responsible for: I hold the main action.
Where it works: ItemDetailsActivity.setupBottomAction shows, hides, or dims btn_claim. Scroll padding leaves room for me.
Why I chose to write the code of the function the way its written: Gravity bottom keeps the button on screen while the photo collapses.
More: I use Widget.ReClaim.Button.Primary.
