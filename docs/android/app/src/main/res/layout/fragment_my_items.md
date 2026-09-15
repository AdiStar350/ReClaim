```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/md_theme_background">
```

What that function does: I am my-items: header, All/Lost/Found tabs, then the list.
What the function is responsible for: I am the root of fragment_my_items.
Where it works: MyItemsFragment inflates me. DashboardActivity and MyItemsActivity host the fragment.
Why I chose to write the code of the function the way its written: Same dark header as home so the tabs match.
More: I use CoordinatorLayout.

```xml
        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_dashboard_header"
            android:clipChildren="false"
            android:clipToPadding="false"
            android:paddingStart="@dimen/screen_padding_horizontal"
            android:paddingTop="@dimen/screen_padding_top"
            android:paddingEnd="@dimen/screen_padding_horizontal"
            android:paddingBottom="@dimen/screen_padding_bottom">

            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="vertical">

                <com.google.android.material.textview.MaterialTextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/my_items_title"
                    android:textAppearance="@style/TextAppearance.ReClaim.DashboardTitle" />

                <com.google.android.material.textview.MaterialTextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/space_4"
                    android:text="@string/my_items_subtitle"
                    android:textAppearance="@style/TextAppearance.ReClaim.HeroSubtitle" />
            </LinearLayout>
        </FrameLayout>
```

What that function does: I show My items and a short subtitle.
What the function is responsible for: I am the my-items hero.
Where it works: MyItemsFragment shows me at the top.
Why I chose to write the code of the function the way its written: Static XML text is enough. There is no greeting to load.
More: I use bg_dashboard_header.

```xml
        <com.google.android.material.tabs.TabLayout
            android:id="@+id/tab_layout"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/md_theme_background"
            app:tabIndicatorColor="@color/brand_accent"
            app:tabIndicatorFullWidth="false"
            app:tabIndicatorHeight="@dimen/space_3"
            app:tabRippleColor="@color/chip_filter_background"
            app:tabSelectedTextColor="@color/brand_dark"
            app:tabTextAppearance="?attr/textAppearanceLabelLarge"
            app:tabTextColor="@color/brand_slate" />
```

What that function does: I am All / Lost / Found.
What the function is responsible for: I filter the list by type.
Where it works: MyItemsFragment adds the three tabs in Java and listens for selection.
Why I chose to write the code of the function the way its written: Tabs are added in code so the labels stay in strings.xml. A short indicator matches the rest of the app.
More: I use TabLayout.

```xml
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recycler_my_items"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_marginTop="@dimen/space_8"
            android:layout_weight="1"
            android:clipToPadding="false"
            android:paddingStart="@dimen/space_16"
            android:paddingEnd="@dimen/space_16"
            android:paddingBottom="@dimen/space_96" />
```

What that function does: I am the my-reports list.
What the function is responsible for: I show item_my_report.xml rows.
Where it works: MyItemsFragment sets MyReportAdapter on recycler_my_items.
Why I chose to write the code of the function the way its written: Bottom padding leaves room for the dashboard bar.
More: I use RecyclerView.

```xml
        <com.google.android.material.textview.MaterialTextView
            android:id="@+id/text_empty"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:gravity="center"
            android:paddingTop="@dimen/space_48"
            android:paddingBottom="@dimen/space_48"
            android:text="@string/my_items_empty"
            android:textColor="@color/text_secondary"
            android:visibility="gone" />

    <ProgressBar
        android:id="@+id/progress"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:indeterminateTint="@color/brand_accent"
        android:visibility="gone" />
```

What that function does: I am the empty text and the load spinner.
What the function is responsible for: Empty when there are no reports. Spinner while fetching.
Where it works: MyItemsFragment toggles text_empty and progress.
Why I chose to write the code of the function the way its written: Both start gone.
More: I use my_items_empty.
