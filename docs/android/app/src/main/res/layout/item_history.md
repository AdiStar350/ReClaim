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

What that function does: I am one history row card.
What the function is responsible for: I am the root of item_history.
Where it works: HistoryAdapter inflates me. ReportHistoryActivity hosts that adapter.
Why I chose to write the code of the function the way its written: Bottom margin spaces the list. A light stroke keeps the card on the page.
More: I use MaterialCardView.

```xml
        <com.google.android.material.card.MaterialCardView
            android:layout_width="@dimen/space_56"
            android:layout_height="@dimen/space_56"
            app:cardBackgroundColor="@color/md_theme_surfaceContainerHigh"
            app:cardCornerRadius="@dimen/space_12"
            app:cardElevation="0dp"
            app:strokeWidth="0dp">

            <ImageView
                android:id="@+id/image_history"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:contentDescription="@null"
                android:scaleType="centerCrop"
                android:src="@drawable/ic_item_placeholder" />
        </com.google.android.material.card.MaterialCardView>
```

What that function does: I show a small rounded thumbnail.
What the function is responsible for: I am the photo on the history row.
Where it works: HistoryAdapter.bind loads image_history with Glide.
Why I chose to write the code of the function the way its written: 56dp fits a dense list. Nested card clips the image.
More: I use ic_item_placeholder until Glide loads.

```xml
            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_history_role"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textAppearance="?attr/textAppearanceLabelMedium"
                android:textColor="@color/text_secondary"
                tools:text="You reported" />

            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_history_title"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_2"
                android:ellipsize="end"
                android:maxLines="2"
                android:textAppearance="?attr/textAppearanceTitleMedium"
                android:textColor="@color/brand_dark"
                tools:text="Black Leather Wallet" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_history_contact"
                style="@style/Widget.Material3.Button.TextButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:minHeight="0dp"
                android:paddingStart="@dimen/space_4"
                android:paddingEnd="@dimen/space_4"
                android:text="@string/btn_view_contact_info"
                android:visibility="gone"
                tools:visibility="visible" />
```

What that function does: I show role, title, and an optional contact button.
What the function is responsible for: I am the middle text block of the history row.
Where it works: HistoryAdapter.bind sets the texts and shows btn_history_contact for approved claims.
Why I chose to write the code of the function the way its written: The button starts gone so report rows stay quiet.
More: I use btn_view_contact_info.

```xml
        <com.google.android.material.textview.MaterialTextView
            android:id="@+id/text_history_status"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/space_10"
            android:background="@drawable/bg_status_badge"
            android:paddingStart="@dimen/space_12"
            android:paddingTop="@dimen/space_5"
            android:paddingEnd="@dimen/space_12"
            android:paddingBottom="@dimen/space_5"
            android:textAppearance="?attr/textAppearanceLabelMedium"
            android:textStyle="bold"
            tools:text="Pending" />
```

What that function does: I show Pending, Approved, or Declined as a small badge.
What the function is responsible for: I am the status chip on the history row.
Where it works: HistoryAdapter.bind sets text and tint from HistoryAdapter.Status.
Why I chose to write the code of the function the way its written: Background tint in Java reuses one drawable for three colors.
More: I use bg_status_badge.
