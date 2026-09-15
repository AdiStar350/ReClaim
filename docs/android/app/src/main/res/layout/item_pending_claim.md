```xml
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="@dimen/space_14"
    app:cardBackgroundColor="@color/md_theme_surfaceContainerLowest"
    app:cardCornerRadius="@dimen/space_18"
    app:cardElevation="@dimen/space_2"
    app:strokeColor="@color/md_theme_outlineVariant"
    app:strokeWidth="@dimen/space_0_5">
```

What that function does: I am one pending claim: photo, title, claimant, answer, reject and approve.
What the function is responsible for: I am the root of item_pending_claim.
Where it works: PendingClaimAdapter inflates me for PendingClaimsSheet and PendingClaimsActivity.
Why I chose to write the code of the function the way its written: Review happens on the card so the sheet does not need extra dialogs.
More: I use a vertical LinearLayout.

```xml
        <View
            android:layout_width="match_parent"
            android:layout_height="@dimen/space_8"
            android:background="@color/brand_accent" />
```

What that function does: I am a top accent strip.
What the function is responsible for: I mark the card as waiting for review.
Where it works: I sit on every pending-claim row.
Why I chose to write the code of the function the way its written: A top bar is easy to scan in a sheet list.
More: I use brand_accent.

```xml
                <com.google.android.material.card.MaterialCardView
                    android:layout_width="@dimen/space_60"
                    android:layout_height="@dimen/space_60"
                    app:cardBackgroundColor="@color/md_theme_surfaceContainer"
                    app:cardCornerRadius="@dimen/space_14"
                    app:cardElevation="0dp"
                    app:strokeWidth="0dp">

                    <ImageView
                        android:id="@+id/image_item_thumbnail"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:contentDescription="@string/item_details_title"
                        android:scaleType="centerCrop"
                        android:src="@drawable/ic_item_placeholder" />
                </com.google.android.material.card.MaterialCardView>
```

What that function does: I am the item thumbnail.
What the function is responsible for: I show which report was claimed.
Where it works: PendingClaimAdapter loads image_item_thumbnail.
Why I chose to write the code of the function the way its written: Same 60dp size as my-report rows.
More: I use centerCrop.

```xml
                    <com.google.android.material.textview.MaterialTextView
                        android:id="@+id/text_item_title"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:ellipsize="end"
                        android:maxLines="2"
                        android:textAppearance="?attr/textAppearanceTitleMedium"
                        android:textColor="@color/brand_dark"
                        tools:text="Black Wallet" />

                    <com.google.android.material.textview.MaterialTextView
                        android:id="@+id/text_claimant"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="@dimen/space_4"
                        android:textAppearance="?attr/textAppearanceBodyMedium"
                        android:textColor="@color/text_secondary"
                        tools:text="From Ada" />
```

What that function does: I show the item title and who claimed it.
What the function is responsible for: I am the claim heading.
Where it works: PendingClaimAdapter sets text_item_title and text_claimant.
Why I chose to write the code of the function the way its written: Two title lines help a long item name. Claimant stays secondary.
More: I use maxLines 2.

```xml
            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_validation_answer"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_12"
                android:textAppearance="?attr/textAppearanceBodyMedium"
                android:textColor="@color/brand_slate"
                tools:text="Answer: Wallpaper with mountains" />
```

What that function does: I show the claimant's check-question answer.
What the function is responsible for: I am the review text.
Where it works: PendingClaimAdapter sets text_validation_answer.
Why I chose to write the code of the function the way its written: The owner can read the answer here instead of opening another dialog.
More: I use brand_slate.

```xml
                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btn_reject_claim"
                    style="@style/Widget.Material3.Button.OutlinedButton"
                    android:layout_width="0dp"
                    android:layout_height="@dimen/button_height"
                    android:layout_marginEnd="@dimen/space_8"
                    android:layout_weight="1"
                    android:text="@string/btn_reject_claim"
                    android:textColor="@color/type_lost"
                    app:strokeColor="@color/type_lost" />

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btn_approve_claim"
                    style="@style/Widget.ReClaim.Button.Primary"
                    android:layout_width="0dp"
                    android:layout_height="@dimen/button_height"
                    android:layout_weight="1"
                    android:text="@string/btn_approve_claim" />
```

What that function does: I am reject and approve, equal width.
What the function is responsible for: I send the review.
Where it works: PendingClaimAdapter wires both buttons. PendingClaimsSheet.Listener updates the remaining list.
Why I chose to write the code of the function the way its written: Reject is outlined red. Approve is the primary fill.
More: I use layout_weight 1 on both.
