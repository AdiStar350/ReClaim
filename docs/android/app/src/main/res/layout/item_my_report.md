```xml
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="@dimen/space_4"
    android:layout_marginBottom="@dimen/space_10"
    app:cardBackgroundColor="@color/md_theme_surfaceContainerLowest"
    app:cardCornerRadius="@dimen/space_18"
    app:cardElevation="@dimen/space_2"
    app:strokeColor="@color/md_theme_outlineVariant"
    app:strokeWidth="@dimen/space_0_5">
```

What that function does: I am one of my reports: thumbnail, title, type, category, place, edit and delete.
What the function is responsible for: I am the root of item_my_report.
Where it works: MyReportAdapter inflates me for MyItemsFragment.
Why I chose to write the code of the function the way its written: A side accent bar marks these as mine, unlike search cards.
More: I use a horizontal LinearLayout.

```xml
        <View
            android:layout_width="@dimen/space_5"
            android:layout_height="match_parent"
            android:background="@color/brand_accent" />
```

What that function does: I am a thin accent strip on the left.
What the function is responsible for: I mark the row as a my-item card.
Where it works: I sit in every item_my_report row.
Why I chose to write the code of the function the way its written: A bar is quieter than a full accent fill.
More: I use brand_accent.

```xml
            <com.google.android.material.card.MaterialCardView
                android:layout_width="@dimen/space_60"
                android:layout_height="@dimen/space_60"
                app:cardBackgroundColor="@color/md_theme_surfaceContainer"
                app:cardCornerRadius="@dimen/space_14"
                app:cardElevation="0dp">

                <ImageView
                    android:id="@+id/image_thumbnail"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:contentDescription="@string/item_details_title"
                    android:scaleType="centerCrop"
                    android:src="@drawable/ic_item_placeholder" />
            </com.google.android.material.card.MaterialCardView>
```

What that function does: I am the small photo.
What the function is responsible for: I show the report image.
Where it works: MyReportAdapter loads image_thumbnail with Glide.
Why I chose to write the code of the function the way its written: 60dp keeps the row short so edit and delete fit.
More: I use centerCrop.

```xml
                    <com.google.android.material.textview.MaterialTextView
                        android:id="@+id/text_title"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:ellipsize="end"
                        android:maxLines="1"
                        android:textAppearance="?attr/textAppearanceTitleMedium"
                        android:textColor="@color/text_primary"
                        tools:text="Black Wallet" />

                    <com.google.android.material.chip.Chip
                        android:id="@+id/chip_type"
                        style="@style/Widget.ReClaim.Chip.Badge"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="@dimen/space_8"
                        android:textColor="@color/brand_white"
                        android:textSize="@dimen/text_size_11"
                        tools:chipBackgroundColor="@color/type_lost_text"
                        tools:text="Lost" />
```

What that function does: I show the title and lost/found.
What the function is responsible for: I am the row heading.
Where it works: MyReportAdapter sets text_title and chip_type.
Why I chose to write the code of the function the way its written: Title takes leftover width. Chip stays on the right.
More: I use Chip.Badge.

```xml
                <com.google.android.material.textview.MaterialTextView
                    android:id="@+id/text_category"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/space_4"
                    android:textAppearance="?attr/textAppearanceLabelLarge"
                    android:textColor="@color/brand_accent"
                    tools:text="Wallets" />

                <com.google.android.material.textview.MaterialTextView
                    android:id="@+id/text_location"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/space_2"
                    android:ellipsize="end"
                    android:maxLines="1"
                    android:textAppearance="?attr/textAppearanceBodySmall"
                    android:textColor="@color/text_secondary"
                    tools:text="Central Station" />
```

What that function does: I show category and place.
What the function is responsible for: I am the extra lines under the title.
Where it works: MyReportAdapter sets text_category and text_location.
Why I chose to write the code of the function the way its written: Accent category matches search cards.
More: I use ellipsize on location.

```xml
                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btn_edit"
                        style="@style/Widget.Material3.Button.TextButton"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:minWidth="0dp"
                        android:paddingStart="@dimen/space_12"
                        android:paddingEnd="@dimen/space_4"
                        android:text="@string/btn_edit"
                        android:textColor="@color/brand_accent" />

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btn_delete"
                        style="@style/Widget.Material3.Button.TextButton"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:minWidth="0dp"
                        android:paddingStart="@dimen/space_4"
                        android:paddingEnd="0dp"
                        android:text="@string/btn_delete"
                        android:textColor="@color/type_lost" />
```

What that function does: I am edit and delete.
What the function is responsible for: Edit opens ReportActivity with EXTRA_ITEM_ID. Delete asks then calls the API.
Where it works: MyReportAdapter wires btn_edit and btn_delete. MyItemsFragment handles the callbacks.
Why I chose to write the code of the function the way its written: Text buttons keep the row short. Delete uses the lost red.
More: I align them at the end.
