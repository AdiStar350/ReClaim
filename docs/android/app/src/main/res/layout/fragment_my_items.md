# fragment_my_items.xml

## Overview
This layout is the "My Items" tab: a header, a tab strip to filter by All / Lost /
Found, a list of your own reports, an empty message, and a loading spinner. It is
inflated by `MyItemsFragment` through `FragmentMyItemsBinding` (hosted by
`MyItemsActivity` via `activity_fragment_host.xml`).

Key idea: a **TabLayout** filters the same **RecyclerView** list by category. See
`docs/GLOSSARY.md`.

## Concepts used here
- **CoordinatorLayout / LinearLayout** — the root and the main column. See `docs/GLOSSARY.md`.
- **FrameLayout** — the colored header block. See `docs/GLOSSARY.md`.
- **TabLayout** — the All/Lost/Found tabs. See `docs/GLOSSARY.md`.
- **RecyclerView** — the list of your reports. See `docs/GLOSSARY.md`.
- **ProgressBar** — the loading spinner. See `docs/GLOSSARY.md`.
- **Adapter + ViewHolder** — `MyReportAdapter` feeds rows from `item_my_report.xml`. See `docs/GLOSSARY.md`.
- **ViewBinding** — `FragmentMyItemsBinding`. See `docs/GLOSSARY.md`.

## Screen structure

```
CoordinatorLayout (root)
├── LinearLayout (vertical)
│   ├── FrameLayout (header: "My items" title + subtitle)
│   ├── TabLayout tab_layout (All / Lost / Found)
│   ├── RecyclerView recycler_my_items (weight = 1)
│   └── MaterialTextView text_empty (hidden)
└── ProgressBar progress (centered over everything)
```

The root is a `CoordinatorLayout` so the spinner can float centered over the whole page,
while the column (`LinearLayout`) lays out the header, tabs, and list top to bottom.

## Components

### Header
```xml
<FrameLayout android:background="@drawable/bg_dashboard_header">
    <LinearLayout android:orientation="vertical">
        <com.google.android.material.textview.MaterialTextView android:text="@string/my_items_title" ... />
        <com.google.android.material.textview.MaterialTextView android:text="@string/my_items_subtitle" ... />
    </LinearLayout>
</FrameLayout>
```
- **What it shows:** a colored header with the "My items" title and a subtitle.
- **Why it's set up this way:** it reuses the same dashboard header background as the
  other tabs for a consistent look; the text is static so no ids are needed here.
- **How the code uses it:** nothing — it is display-only.

### Filter tabs
```xml
<com.google.android.material.tabs.TabLayout
    android:id="@+id/tab_layout"
    app:tabIndicatorColor="@color/brand_accent"
    app:tabSelectedTextColor="@color/brand_dark" />
```
- **What it shows:** a row of tabs (All reports / Lost / Found) with an accent underline
  on the selected tab.
- **Why it's set up this way:** the tabs are added in code rather than in XML, so the
  layout just defines the styled strip. A `TabLayout` is the standard Material way to
  filter a list by category.
- **How the code uses it:** the code adds the three tabs
  (`binding.tabLayout.addTab(...)`) and listens for tab changes
  (`addOnTabSelectedListener`) to filter which reports the list shows.

### Reports list and empty state
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recycler_my_items"
    android:layout_height="0dp"
    android:layout_weight="1"
    android:clipToPadding="false"
    android:paddingBottom="@dimen/space_96" />

<com.google.android.material.textview.MaterialTextView
    android:id="@+id/text_empty"
    android:text="@string/my_items_empty"
    android:visibility="gone" />
```
- **What it shows:** the scrollable list of your reports and a "no items" message when
  the current filter has none.
- **Why it's set up this way:** `height="0dp"` + `layout_weight="1"` makes the list take
  all the space left under the tabs. The large bottom padding keeps the last card clear
  of the dashboard's bottom bar / floating button.
- **How the code uses it:** the code sets a `LinearLayoutManager` and `MyReportAdapter`
  on `binding.recyclerMyItems`, and flips `binding.textEmpty` and
  `binding.recyclerMyItems` visibility depending on whether the filtered list is empty.

### Loading spinner
```xml
<ProgressBar
    android:id="@+id/progress"
    android:layout_gravity="center"
    android:visibility="gone" />
```
- **What it shows:** a centered spinner while reports load.
- **Why it's set up this way:** it is a direct child of the root `CoordinatorLayout` with
  `layout_gravity="center"` so it floats in the middle of the whole screen, above the
  list; it starts `gone`.
- **How the code uses it:** the code toggles `binding.progress` visibility while loading.
