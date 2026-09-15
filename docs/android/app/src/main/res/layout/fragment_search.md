```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/md_theme_background">
```

What that function does: I am the search screen: toolbar, search box, then list or map.
What the function is responsible for: I am the root of fragment_search.
Where it works: SearchFragment inflates me. DashboardActivity and SearchActivity host the fragment.
Why I chose to write the code of the function the way its written: A vertical LinearLayout under the coordinator keeps header and content stacked.
More: I use CoordinatorLayout.

```xml
        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar_search"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/brand_dark"
            android:minHeight="@dimen/space_60"
            android:paddingStart="@dimen/screen_padding_horizontal"
            android:paddingEnd="@dimen/screen_padding_horizontal"
            android:paddingTop="@dimen/screen_padding_top"
            android:paddingBottom="@dimen/screen_padding_bottom"
            app:title="@string/search_title"
            app:titleTextColor="@color/brand_cream">

            <com.google.android.material.button.MaterialButtonToggleGroup
                android:id="@+id/toggle_view_mode"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="end"
                android:layout_marginEnd="@dimen/space_4"
                app:checkedButton="@+id/btn_view_list"
                app:selectionRequired="true"
                app:singleSelection="true">

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btn_view_list"
                    style="@style/Widget.Material3.Button.OutlinedButton"
                    android:layout_width="wrap_content"
                    android:layout_height="@dimen/space_48"
                    android:minWidth="0dp"
                    android:paddingStart="@dimen/space_14"
                    android:paddingEnd="@dimen/space_14"
                    android:text="@string/view_list"
                    android:textColor="@color/toggle_btn_text"
                    android:textSize="@dimen/text_size_13"
                    app:backgroundTint="@color/toggle_btn_background"
                    app:strokeColor="@color/brand_accent" />

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btn_view_map"
                    style="@style/Widget.Material3.Button.OutlinedButton"
                    android:layout_width="wrap_content"
                    android:layout_height="@dimen/space_48"
                    android:minWidth="0dp"
                    android:paddingStart="@dimen/space_14"
                    android:paddingEnd="@dimen/space_14"
                    android:text="@string/view_map"
                    android:textColor="@color/toggle_btn_text"
                    android:textSize="@dimen/text_size_13"
                    app:backgroundTint="@color/toggle_btn_background"
                    app:strokeColor="@color/brand_accent" />
            </com.google.android.material.button.MaterialButtonToggleGroup>
        </com.google.android.material.appbar.MaterialToolbar>
```

What that function does: I am the search title bar with list/map toggle.
What the function is responsible for: I switch view mode.
Where it works: SearchFragment.updateViewMode listens to toggle_view_mode.
Why I chose to write the code of the function the way its written: singleSelection keeps one mode. The toggle sits in the toolbar so the list can use the rest of the screen.
More: I use MaterialButtonToggleGroup.

```xml
        <com.google.android.material.card.MaterialCardView
            style="@style/Widget.ReClaim.Card"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/screen_padding_horizontal"
            android:layout_marginTop="@dimen/space_neg_10"
            android:layout_marginEnd="@dimen/screen_padding_horizontal"
            app:cardElevation="@dimen/space_6">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:paddingStart="@dimen/space_12"
                android:paddingTop="@dimen/space_10"
                android:paddingEnd="@dimen/space_10"
                android:paddingBottom="@dimen/space_10">

                <com.google.android.material.textfield.TextInputLayout
                    android:id="@+id/input_layout_search"
                    style="@style/Widget.ReClaim.TextInput"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:hint="@string/search_hint"
                    app:startIconDrawable="@android:drawable/ic_menu_search">

                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/edit_search"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:inputType="text"
                        android:textColor="@color/text_primary" />
                </com.google.android.material.textfield.TextInputLayout>

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btn_filter_sort"
                    style="@style/Widget.Material3.Button.IconButton.Filled.Tonal"
                    android:layout_width="@dimen/space_48"
                    android:layout_height="@dimen/space_48"
                    android:layout_marginStart="@dimen/space_8"
                    android:contentDescription="@string/filter_sort_menu"
                    app:backgroundTint="@color/brand_accent"
                    app:icon="@android:drawable/ic_menu_sort_by_size"
                    app:iconTint="@color/text_on_accent" />
            </LinearLayout>

        </com.google.android.material.card.MaterialCardView>
```

What that function does: I am the search field and the filter button.
What the function is responsible for: I take a query and open the filter menu.
Where it works: SearchFragment watches edit_search and btn_filter_sort.
Why I chose to write the code of the function the way its written: Negative top margin overlaps the dark toolbar a little so the card feels attached.
More: I use Widget.ReClaim.TextInput.

```xml
            <com.google.android.material.chip.Chip
                android:id="@+id/chip_active_filters"
                style="@style/Widget.ReClaim.Chip.Sort"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="@dimen/screen_padding_horizontal"
                android:layout_alignParentStart="true"
                android:layout_centerVertical="true"
                android:text="@string/filter_sort_default"
                android:visibility="gone" />
```

What that function does: I show a short summary of the active filters.
What the function is responsible for: I am the filter chip.
Where it works: SearchFragment.applyFilters sets my text and shows me.
Why I chose to write the code of the function the way its written: Start gone until the first apply so an empty chip is not shown.
More: I use Widget.ReClaim.Chip.Sort.

```xml
            <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
                android:id="@+id/swipe_refresh"
                android:layout_width="match_parent"
                android:layout_height="match_parent">

                <androidx.recyclerview.widget.RecyclerView
                    android:id="@+id/recycler_items"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:clipToPadding="false"
                    android:paddingStart="@dimen/screen_padding_horizontal"
                    android:paddingTop="@dimen/space_8"
                    android:paddingEnd="@dimen/screen_padding_horizontal"
                    android:paddingBottom="@dimen/space_96" />
            </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
```

What that function does: I am the item list with pull to refresh.
What the function is responsible for: I show filtered cards.
Where it works: SearchFragment sets ItemAdapter on recycler_items. Swipe calls viewModel.loadItems.
Why I chose to write the code of the function the way its written: Bottom padding leaves room for the dashboard bar when I sit in DashboardActivity.
More: I use SwipeRefreshLayout.

```xml
            <FrameLayout
                android:id="@+id/map_container"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:paddingStart="@dimen/screen_padding_horizontal"
                android:paddingEnd="@dimen/screen_padding_horizontal"
                android:paddingBottom="@dimen/space_50"
                android:clipToPadding="false"
                android:visibility="gone">

                <com.google.android.material.card.MaterialCardView
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    app:cardCornerRadius="@dimen/space_22"
                    app:cardElevation="@dimen/space_2"
                    app:strokeColor="@color/md_theme_outlineVariant"
                    app:strokeWidth="@dimen/space_0_5">

                    <androidx.fragment.app.FragmentContainerView
                        android:id="@+id/map"
                        android:name="com.google.android.gms.maps.SupportMapFragment"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent" />
                </com.google.android.material.card.MaterialCardView>
            </FrameLayout>
```

What that function does: I hold the map in a rounded card. Start hidden.
What the function is responsible for: I am map mode.
Where it works: SearchFragment.onMapReady uses R.id.map. updateViewMode shows me.
Why I chose to write the code of the function the way its written: A card clips the map corners. Start gone so list is the default.
More: I use SupportMapFragment.

```xml
            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_empty"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center"
                android:gravity="center"
                android:paddingStart="@dimen/space_32"
                android:paddingEnd="@dimen/space_32"
                android:text="@string/list_empty"
                android:textColor="@color/text_secondary"
                android:visibility="gone" />
```

What that function does: I say the filtered list is empty.
What the function is responsible for: I am the empty state.
Where it works: SearchFragment.applyFilters shows me when filtered is empty.
Why I chose to write the code of the function the way its written: Center gravity sits over the list or map slot.
More: I use list_empty.
