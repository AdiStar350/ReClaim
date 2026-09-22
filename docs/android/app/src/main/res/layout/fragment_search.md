# fragment_search.xml

## Overview
This layout is the Search tab: a header with a list/map toggle, a search card with a
text field and a filter button, a row that shows the active filter, and a main area that
switches between a pull-to-refresh list and a map. It is inflated by `SearchFragment`
through `FragmentSearchBinding` (hosted by `SearchActivity` via `activity_fragment_host.xml`).

Key idea: one screen with two views of the same data — a **RecyclerView** list and a
Google map — swapped by a toggle. See `docs/GLOSSARY.md`.

## Concepts used here
- **CoordinatorLayout / LinearLayout** — the root and the main column. See `docs/GLOSSARY.md`.
- **MaterialToolbar** — the header bar (also holds the toggle). See `docs/GLOSSARY.md`.
- **MaterialButtonToggleGroup + Material Button** — the List/Map switch. See `docs/GLOSSARY.md`.
- **MaterialCardView** — the search box card and the map frame. See `docs/GLOSSARY.md`.
- **TextInputLayout + TextInputEditText** — the search field. See `docs/GLOSSARY.md`.
- **Chip** — the active-filters pill. See `docs/GLOSSARY.md`.
- **SwipeRefreshLayout + RecyclerView** — the pull-to-refresh results list. See `docs/GLOSSARY.md`.
- **FragmentContainerView** — hosts the map fragment. See `docs/GLOSSARY.md`.
- **ViewBinding** — `FragmentSearchBinding`. See `docs/GLOSSARY.md`.

## Screen structure

```
CoordinatorLayout (root)
└── LinearLayout (vertical)
    ├── MaterialToolbar toolbar_search (title + toggle_view_mode: btn_view_list / btn_view_map)
    ├── Card: search box (input_layout_search / edit_search) + btn_filter_sort
    ├── RelativeLayout search_filter_bar (chip_active_filters)
    └── FrameLayout (fills the rest)
        ├── SwipeRefreshLayout swipe_refresh → RecyclerView recycler_items
        ├── FrameLayout map_container → Card → FragmentContainerView (@id/map)
        └── MaterialTextView text_empty (centered, hidden)
```

The list, map, and empty message share the bottom `FrameLayout` so exactly one is shown
at a time in the same space.

## Components

### Toolbar with List/Map toggle
```xml
<com.google.android.material.appbar.MaterialToolbar android:id="@+id/toolbar_search" ...>
    <com.google.android.material.button.MaterialButtonToggleGroup
        android:id="@+id/toggle_view_mode"
        app:checkedButton="@+id/btn_view_list"
        app:selectionRequired="true"
        app:singleSelection="true">
        <com.google.android.material.button.MaterialButton android:id="@+id/btn_view_list" android:text="@string/view_list" ... />
        <com.google.android.material.button.MaterialButton android:id="@+id/btn_view_map" android:text="@string/view_map" ... />
    </com.google.android.material.button.MaterialButtonToggleGroup>
</com.google.android.material.appbar.MaterialToolbar>
```
- **What it shows:** the "Search" title with a two-button "List / Map" switch on the
  right; List is selected by default.
- **Why it's set up this way:** a `MaterialButtonToggleGroup` with `singleSelection` and
  `selectionRequired` makes the two buttons act like a segmented control — exactly one is
  always on. Placing it inside the toolbar keeps the header compact.
- **How the code uses it:** the code listens on `binding.toggleViewMode` (an
  `addOnButtonCheckedListener`) to switch between showing the list and the map.

### Search box and filter button
```xml
<com.google.android.material.card.MaterialCardView style="@style/Widget.ReClaim.Card">
    <LinearLayout android:orientation="horizontal">
        <com.google.android.material.textfield.TextInputLayout android:id="@+id/input_layout_search" android:layout_weight="1" ...>
            <com.google.android.material.textfield.TextInputEditText android:id="@+id/edit_search" ... />
        </com.google.android.material.textfield.TextInputLayout>
        <com.google.android.material.button.MaterialButton android:id="@+id/btn_filter_sort" app:icon="@android:drawable/ic_menu_sort_by_size" ... />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```
- **What it shows:** a floating search card with a search field (magnifier icon) and a
  round filter/sort button next to it.
- **Why it's set up this way:** the field uses `layout_weight="1"` so it grows to fill
  the row while the fixed-size filter button stays on the right. The card is lifted with a
  negative top margin so it overlaps the header for a modern look.
- **How the code uses it:** the code adds a text watcher to `binding.editSearch` to
  filter as you type, and `binding.btnFilterSort` opens the filter/sort menu.

### Active filters pill
```xml
<RelativeLayout android:id="@+id/search_filter_bar">
    <com.google.android.material.chip.Chip
        android:id="@+id/chip_active_filters"
        style="@style/Widget.ReClaim.Chip.Sort"
        android:text="@string/filter_sort_default"
        android:visibility="gone" />
</RelativeLayout>
```
- **What it shows:** a small chip summarizing the current filter/sort (e.g. "Newest
  first").
- **Why it's set up this way:** it sits in a thin `RelativeLayout` bar aligned to the
  start; it starts `gone` and is shown once results load.
- **How the code uses it:** the code sets `binding.chipActiveFilters` text with a filter
  summary and makes it visible when there are results.

### Results list (pull to refresh)
```xml
<androidx.swiperefreshlayout.widget.SwipeRefreshLayout android:id="@+id/swipe_refresh">
    <androidx.recyclerview.widget.RecyclerView android:id="@+id/recycler_items" ... />
</androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
```
- **What it shows:** the vertical list of item cards, which you can pull down to refresh.
- **Why it's set up this way:** wrapping the `RecyclerView` in a `SwipeRefreshLayout`
  adds the standard pull-to-refresh spinner with no extra UI. Each row uses `item_card.xml`.
- **How the code uses it:** the code sets a `LinearLayoutManager` and adapter on
  `binding.recyclerItems`, and wires `binding.swipeRefresh` to reload items and to show
  the refreshing spinner.

### Map view
```xml
<FrameLayout android:id="@+id/map_container" android:visibility="gone">
    <com.google.android.material.card.MaterialCardView>
        <androidx.fragment.app.FragmentContainerView
            android:id="@+id/map"
            android:name="com.google.android.gms.maps.SupportMapFragment" />
    </com.google.android.material.card.MaterialCardView>
</FrameLayout>
```
- **What it shows:** a Google map (inside a rounded card) with pins for items; hidden
  until you pick "Map".
- **Why it's set up this way:** the map is a `FragmentContainerView` (the standard way to
  embed a map), wrapped in a card for rounded corners. The whole thing starts `gone` so
  only the list shows at first.
- **How the code uses it:** the code toggles `binding.mapContainer` vs
  `binding.swipeRefresh` visibility when the List/Map switch changes.

### Empty state
```xml
<com.google.android.material.textview.MaterialTextView
    android:id="@+id/text_empty"
    android:layout_gravity="center"
    android:text="@string/list_empty"
    android:visibility="gone" />
```
- **What it shows:** a centered "no results" message.
- **Why it's set up this way:** it lives in the same `FrameLayout` as the list/map,
  centered and `gone` until needed.
- **How the code uses it:** the code shows/hides `binding.textEmpty` based on whether the
  results list is empty.
