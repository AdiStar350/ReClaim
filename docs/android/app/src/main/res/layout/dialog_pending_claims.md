```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingBottom="@dimen/space_20">
```

What that function does: I am the pending-claims bottom sheet: handle, header, list.
What the function is responsible for: I am the root of dialog_pending_claims.
Where it works: PendingClaimsSheet inflates me. HomeFragment.showPendingClaimsSheet opens that sheet.
Why I chose to write the code of the function the way its written: wrap_content lets the sheet grow with the claim count.
More: I use a vertical LinearLayout.

```xml
    <View
        android:layout_width="@dimen/space_40"
        android:layout_height="@dimen/space_4"
        android:layout_gravity="center_horizontal"
        android:layout_marginTop="@dimen/space_10"
        android:background="@color/md_theme_outlineVariant" />
```

What that function does: I am the drag handle.
What the function is responsible for: I show this is a sheet.
Where it works: I sit at the top of dialog_pending_claims.
Why I chose to write the code of the function the way its written: A short bar is the usual sheet cue.
More: I use outlineVariant.

```xml
    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/text_pending_claims_header"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingStart="@dimen/space_20"
        android:paddingTop="@dimen/space_16"
        android:paddingEnd="@dimen/space_20"
        android:paddingBottom="@dimen/space_12"
        android:textAppearance="@style/TextAppearance.ReClaim.SectionTitle"
        android:textColor="@color/brand_dark"
        tools:text="Review Pending Claims (2)" />
```

What that function does: I show the sheet title with a count.
What the function is responsible for: I am the pending-claims header.
Where it works: PendingClaimsSheet sets text_pending_claims_header.
Why I chose to write the code of the function the way its written: XML has no default string so Java can put the live count in.
More: I use SectionTitle.

```xml
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recycler_pending_claims"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:clipToPadding="false"
        android:paddingStart="@dimen/space_20"
        android:paddingEnd="@dimen/space_20"
        android:paddingBottom="@dimen/space_8" />
```

What that function does: I am the sheet list of claims.
What the function is responsible for: I show item_pending_claim.xml rows.
Where it works: PendingClaimsSheet sets PendingClaimAdapter on recycler_pending_claims.
Why I chose to write the code of the function the way its written: wrap_content lets the sheet size to the rows. Approve and reject stay on each card.
More: I use RecyclerView.
