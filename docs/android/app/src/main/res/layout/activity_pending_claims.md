```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/md_theme_background"
    android:orientation="vertical">
```

What that function does: I am the full-screen pending-claims list.
What the function is responsible for: I am the root of activity_pending_claims.
Where it works: PendingClaimsActivity inflates me through ActivityPendingClaimsBinding.
Why I chose to write the code of the function the way its written: Toolbar plus list is enough. Empty handling lives in Java on the adapter.
More: I use a vertical LinearLayout.

```xml
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar_pending_claims"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@color/brand_dark"
        app:navigationIcon="@drawable/ic_arrow_back_24"
        app:navigationIconTint="@color/brand_cream"
        app:title="@string/pending_claims_title"
        app:titleTextColor="@color/brand_cream"
        android:paddingTop="@dimen/space_60" />
```

What that function does: I am the dark pending-claims toolbar.
What the function is responsible for: I show the title and back.
Where it works: PendingClaimsActivity uses toolbar_pending_claims.
Why I chose to write the code of the function the way its written: paddingTop 60 covers the status bar like report history.
More: I use brand_dark.

```xml
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recycler_pending_claims"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:clipToPadding="false"
        android:padding="@dimen/space_20" />
```

What that function does: I am the pending-claims list.
What the function is responsible for: I show item_pending_claim.xml rows.
Where it works: PendingClaimsActivity sets PendingClaimAdapter on recycler_pending_claims.
Why I chose to write the code of the function the way its written: Padding 20 matches report history.
More: I use RecyclerView.
