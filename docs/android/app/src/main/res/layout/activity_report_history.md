```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/md_theme_background"
    android:orientation="vertical">
```

What that function does: I am closed-report history: toolbar, list, empty, spinner.
What the function is responsible for: I am the root of activity_report_history.
Where it works: ReportHistoryActivity inflates me through ActivityReportHistoryBinding.
Why I chose to write the code of the function the way its written: A FrameLayout under the toolbar stacks list, empty, and spinner.
More: I use a vertical LinearLayout.

```xml
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar_report_history"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@color/brand_dark"
        android:paddingTop="@dimen/space_60"
        app:navigationIcon="@drawable/ic_arrow_back_24"
        app:navigationIconTint="@color/brand_cream"
        app:title="@string/report_history_title"
        app:titleTextColor="@color/brand_cream" />
```

What that function does: I am the dark history toolbar.
What the function is responsible for: I show Report history and back.
Where it works: ReportHistoryActivity uses toolbar_report_history.
Why I chose to write the code of the function the way its written: paddingTop 60 covers the status bar like report and pending claims.
More: I use brand_dark.

```xml
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recycler_report_history"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:clipToPadding="false"
            android:padding="@dimen/space_20" />
```

What that function does: I am the history list.
What the function is responsible for: I show item_history.xml rows.
Where it works: ReportHistoryActivity sets HistoryAdapter on recycler_report_history.
Why I chose to write the code of the function the way its written: Padding 20 matches pending claims.
More: I use RecyclerView.

```xml
        <com.google.android.material.textview.MaterialTextView
            android:id="@+id/text_history_empty"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:gravity="center"
            android:paddingStart="@dimen/space_40"
            android:paddingEnd="@dimen/space_40"
            android:text="@string/report_history_empty"
            android:textAppearance="?attr/textAppearanceBodyLarge"
            android:textColor="@color/text_secondary"
            android:visibility="gone" />

        <ProgressBar
            android:id="@+id/progress_report_history"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:indeterminateTint="@color/brand_accent"
            android:visibility="gone" />
```

What that function does: I am empty text and the load spinner, both centered.
What the function is responsible for: Empty when there are no closed reports.
Where it works: ReportHistoryActivity toggles text_history_empty and progress_report_history.
Why I chose to write the code of the function the way its written: Both start gone so the list can fill the frame.
More: I use report_history_empty.
