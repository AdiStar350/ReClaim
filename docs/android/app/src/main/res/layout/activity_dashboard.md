```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/brand_dark"
    android:fitsSystemWindows="false">
```

What that function does: I am the signed-in shell: a fragment slot, a bottom bar, and a report FAB.
What the function is responsible for: I am the root of activity_dashboard.
Where it works: DashboardActivity inflates me through ActivityDashboardBinding.
Why I chose to write the code of the function the way its written: CoordinatorLayout lets the FAB sit over the bar. fitsSystemWindows false lets the fragments paint their own headers.
More: I use CoordinatorLayout.

```xml
    <FrameLayout
        android:id="@+id/fragment_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_marginBottom="@dimen/bottom_nav_height" />
```

What that function does: I host Home, Search, My Items, and Profile fragments.
What the function is responsible for: I am the tab content slot.
Where it works: DashboardActivity.showFragment puts fragments in me.
Why I chose to write the code of the function the way its written: Bottom margin keeps content above the bar.
More: I use fragment_container.

```xml
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_navigation"
        style="@style/Widget.ReClaim.BottomNavigation"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        android:background="@drawable/bg_bottom_nav"
        app:itemActiveIndicatorStyle="@style/Widget.ReClaim.BottomNav.ActiveIndicator"
        app:itemIconTint="@color/bottom_nav_item_color"
        app:itemTextColor="@color/bottom_nav_item_color"
        app:elevation="@dimen/space_12"
        app:labelVisibilityMode="labeled"
        app:menu="@menu/bottom_nav_menu"
        app:paddingBottomSystemWindowInsets="false"
        app:paddingTopSystemWindowInsets="false" />
```

What that function does: I am the four-tab bar. The middle item is a gap for the FAB.
What the function is responsible for: I switch tabs.
Where it works: DashboardActivity.setOnItemSelectedListener reads me. nav_report_placeholder is ignored.
Why I chose to write the code of the function the way its written: labeled keeps names visible. Insets are off so DashboardActivity can pad the bar itself.
More: I use bottom_nav_menu.

```xml
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fab_add_report"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="@dimen/space_40"
        android:contentDescription="@string/cd_create_report"
        app:backgroundTint="@color/brand_accent"
        app:borderWidth="0dp"
        app:elevation="@dimen/space_16"
        app:fabCustomSize="@dimen/space_64"
        app:maxImageSize="@dimen/space_28"
        app:srcCompat="@drawable/ic_add_24"
        app:tint="@color/text_on_accent" />
```

What that function does: I am the add-report button sitting in the middle of the bar.
What the function is responsible for: I open a new report.
Where it works: DashboardActivity starts ReportActivity from fab_add_report.
Why I chose to write the code of the function the way its written: A custom 64dp size sits in the empty middle slot. Accent color matches the brand.
More: I use FloatingActionButton.
