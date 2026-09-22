# activity_report_history.xml

## Overview
This layout is the "report history" screen: a dark toolbar and a list of your past
reports and claims, with an empty-state message and a loading spinner. It is inflated by
`ReportHistoryActivity` through `ActivityReportHistoryBinding`.

Key idea: a list screen built from a **RecyclerView**, where an overlapping
`FrameLayout` lets the empty message and spinner sit centered on top of the list. See
`docs/GLOSSARY.md`.

## Concepts used here
- **LinearLayout** — vertical stack (toolbar over the content area). See `docs/GLOSSARY.md`.
- **MaterialToolbar** — top bar with a back arrow and title. See `docs/GLOSSARY.md`.
- **FrameLayout** — overlaps the list, empty text, and spinner in one region. See `docs/GLOSSARY.md`.
- **RecyclerView** — the scrollable history list. See `docs/GLOSSARY.md`.
- **ProgressBar** — the centered loading spinner. See `docs/GLOSSARY.md`.
- **Adapter + ViewHolder** — `HistoryAdapter` feeds rows from `item_history.xml`. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityReportHistoryBinding`. See `docs/GLOSSARY.md`.

## Screen structure

```
LinearLayout (root, vertical)
├── MaterialToolbar (toolbar_report_history)
└── FrameLayout (fills the rest)
    ├── RecyclerView (recycler_report_history)
    ├── MaterialTextView (text_history_empty — centered, hidden by default)
    └── ProgressBar (progress_report_history — centered, hidden by default)
```

The list, empty message, and spinner share one `FrameLayout` so only one is visible at a
time in the same centered space.

## Components

### Toolbar
```xml
<com.google.android.material.appbar.MaterialToolbar
    android:id="@+id/toolbar_report_history"
    android:background="@color/brand_dark"
    android:paddingTop="@dimen/space_60"
    app:navigationIcon="@drawable/ic_arrow_back_24"
    app:title="@string/report_history_title" />
```
- **What it shows:** a dark bar with a back arrow and the "Report history" title.
- **Why it's set up this way:** the top padding clears the status bar; the dark color
  matches the app's other headers.
- **How the code uses it:** the code sets it as the action bar
  (`setSupportActionBar(binding.toolbarReportHistory)`) and finishes the screen from its
  navigation click listener.

### List, empty state, and spinner
```xml
<FrameLayout android:layout_width="match_parent" android:layout_height="match_parent">
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recycler_report_history"
        android:clipToPadding="false"
        android:padding="@dimen/space_20" />

    <com.google.android.material.textview.MaterialTextView
        android:id="@+id/text_history_empty"
        android:layout_gravity="center"
        android:text="@string/report_history_empty"
        android:visibility="gone" />

    <ProgressBar
        android:id="@+id/progress_report_history"
        android:layout_gravity="center"
        android:visibility="gone" />
</FrameLayout>
```
- **What it shows:** the scrollable history list; a centered "no history yet" message
  when the list is empty; and a centered spinner while loading.
- **Why it's set up this way:** stacking all three in a `FrameLayout` with
  `layout_gravity="center"` on the message and spinner is the simplest way to show
  exactly one state in the middle of the screen. Both extras start `gone`.
- **How the code uses it:** the code puts a `LinearLayoutManager` and `HistoryAdapter`
  on `binding.recyclerReportHistory`. While loading it shows
  `binding.progressReportHistory` and hides `binding.textHistoryEmpty`; when results
  arrive it hides the spinner and shows the empty text only if there are no items.
