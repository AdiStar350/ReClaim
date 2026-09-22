# activity_pending_claims.xml

## Overview
This layout is the full-screen "pending claims" list: a dark toolbar and a scrollable
list of claims other people have made on your found items, so you can review them. It is
inflated by `PendingClaimsActivity` through `ActivityPendingClaimsBinding`.

Key idea: a **RecyclerView** is a scrollable list that reuses row views for speed; each
row here comes from `item_pending_claim.xml`. See `docs/GLOSSARY.md`.

## Concepts used here
- **LinearLayout** — vertical stack (toolbar over list). See `docs/GLOSSARY.md`.
- **MaterialToolbar** — the top bar with a back arrow and title. See `docs/GLOSSARY.md`.
- **RecyclerView** — the scrollable list of claim rows. See `docs/GLOSSARY.md`.
- **Adapter + ViewHolder** — `PendingClaimAdapter` feeds the rows. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityPendingClaimsBinding`. See `docs/GLOSSARY.md`.

## Screen structure
A short, simple screen:

```
LinearLayout (root, vertical)
├── MaterialToolbar (toolbar_pending_claims)
└── RecyclerView (recycler_pending_claims — fills the rest)
```

The root is a `LinearLayout` because it is just a bar on top of a list.

## Components

### Toolbar
```xml
<com.google.android.material.appbar.MaterialToolbar
    android:id="@+id/toolbar_pending_claims"
    android:background="@color/brand_dark"
    app:navigationIcon="@drawable/ic_arrow_back_24"
    app:title="@string/pending_claims_title"
    android:paddingTop="@dimen/space_60" />
```
- **What it shows:** a dark bar with a back arrow and the "Pending claims" title.
- **Why it's set up this way:** the top padding pushes the content below the status bar
  (this screen doesn't use `fitsSystemWindows`); the dark color matches the app's headers.
- **How the code uses it:** `PendingClaimsActivity` sets it as the action bar with
  `setSupportActionBar(binding.toolbarPendingClaims)` and closes the screen from
  `binding.toolbarPendingClaims.setNavigationOnClickListener(v -> finish())`.

### Claims list
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recycler_pending_claims"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:clipToPadding="false"
    android:padding="@dimen/space_20" />
```
- **What it shows:** the vertical list of claim cards to approve or reject.
- **Why it's set up this way:** `clipToPadding="false"` lets rows scroll into the padding
  area (so the first/last cards aren't clipped against the edges) while keeping the
  padding as a comfortable margin.
- **How the code uses it:** the code sets a `LinearLayoutManager` and the adapter on
  `binding.recyclerPendingClaims` (`setLayoutManager(...)`, `setAdapter(adapter)`); the
  adapter inflates one `item_pending_claim.xml` per claim.
