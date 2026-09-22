# activity_dashboard.xml

## Overview
This layout is the main app shell after login: a full-screen area that swaps in
fragments (Home, Search, My Items, Profile), a bottom navigation bar, and a floating
"+" button that sits in the middle notch of that bar. It is inflated by
`DashboardActivity` through `ActivityDashboardBinding`.

Key idea: this is the container screen; the actual pages are **Fragments** shown inside
`fragment_container`, chosen by the bottom nav. See `docs/GLOSSARY.md`.

## Concepts used here
- **CoordinatorLayout** — a container that lets children overlap and coordinate (here,
  the FAB floats above the bottom bar). See `docs/GLOSSARY.md`.
- **FrameLayout** — a simple container that holds the current fragment. See `docs/GLOSSARY.md`.
- **BottomNavigationView** — the Material bottom tab bar. See `docs/GLOSSARY.md`.
- **FloatingActionButton (FAB)** — the round "+" action button. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityDashboardBinding`. See `docs/GLOSSARY.md`.

## Screen structure
The root is a `CoordinatorLayout` so its three children can stack on top of each other:

```
CoordinatorLayout (root)
├── FrameLayout  (@id/fragment_container — fills the screen, holds the active fragment)
├── BottomNavigationView  (@id/bottom_navigation — pinned to the bottom)
└── FloatingActionButton  (@id/fab_add_report — centered, floating over the bar)
```

A `CoordinatorLayout` is used (instead of a plain `LinearLayout`) because the FAB has to
float over the bottom bar in the same space, not sit above or below it in a column.

## Components

### Fragment container
```xml
<FrameLayout
    android:id="@+id/fragment_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_marginBottom="@dimen/bottom_nav_height" />
```
- **What it shows:** the region where the current page (Home/Search/My Items/Profile)
  is displayed. It starts empty in XML and is filled with a fragment at runtime.
- **Why it's set up this way:** a `FrameLayout` is the standard "put one fragment here"
  container. The bottom margin equal to the nav height keeps page content from hiding
  behind the bottom bar.
- **How the code uses it:** `DashboardActivity` swaps fragments into
  `binding.fragmentContainer`, and adjusts its top padding/margins to fit under the
  status bar using window insets.

### Bottom navigation bar
```xml
<com.google.android.material.bottomnavigation.BottomNavigationView
    android:id="@+id/bottom_navigation"
    style="@style/Widget.ReClaim.BottomNavigation"
    android:layout_gravity="bottom"
    android:background="@drawable/bg_bottom_nav"
    app:labelVisibilityMode="labeled"
    app:menu="@menu/bottom_nav_menu" />
```
- **What it shows:** the tab bar at the bottom with icons and labels for the main
  sections.
- **Why it's set up this way:** `layout_gravity="bottom"` pins it to the bottom of the
  `CoordinatorLayout`. Its tabs come from a menu resource (`bottom_nav_menu`) rather than
  being hand-written here, which keeps the layout short and the tabs reusable.
- **How the code uses it:** `binding.bottomNavigation.setOnItemSelectedListener(...)`
  switches fragments when a tab is tapped, and `setSelectedItemId(...)` selects a tab in
  code (for example, defaulting to Home). It also gets bottom padding from window insets.

### Floating "+" button
```xml
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/fab_add_report"
    android:layout_gravity="bottom|center_horizontal"
    android:layout_marginBottom="@dimen/space_40"
    app:backgroundTint="@color/brand_accent"
    app:srcCompat="@drawable/ic_add_24" />
```
- **What it shows:** a round accent-colored "+" button centered over the bottom bar,
  used to create a new report.
- **Why it's set up this way:** `layout_gravity="bottom|center_horizontal"` plus a bottom
  margin lifts it into the empty middle slot of the bottom nav, a common Material pattern.
  Its elevation is higher than the bar so it appears to float above it.
- **How the code uses it:** `binding.fabAddReport.setOnClickListener(...)` opens the
  report screen (`ReportActivity`). The FAB's margins are also nudged with window insets.
