# activity_fragment_host.xml

## Overview
This layout is a tiny, reusable "shell" screen: just one empty container that an
Activity fills with a fragment at runtime. It is used (via
`setContentView(R.layout.activity_fragment_host)`) by three Activities:
`SearchActivity` (hosts `SearchFragment`), `ProfileActivity` (hosts
`ProfileSettingsFragment`), and `MyItemsActivity` (hosts `MyItemsFragment`). Because it
has no named views to bind, these Activities use `R.layout` and `R.id` directly instead
of a generated binding.

Key idea: a **Fragment** is a reusable piece of UI that lives inside an Activity; this
layout is the empty slot it lives in. See `docs/GLOSSARY.md`.

## Concepts used here
- **FrameLayout** — a single-slot container that holds one fragment. See `docs/GLOSSARY.md`.
- **Fragment hosting** — an Activity swaps a fragment into this container. See `docs/GLOSSARY.md`.

## Screen structure
The whole file is one view:

```
FrameLayout (@id/fragment_host — fills the screen)
```

A `FrameLayout` is the right root because it is the standard "put one fragment here"
container: it holds a single child that fills the space.

## Components

### Fragment host container
```xml
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/fragment_host"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/md_theme_background" />
```
- **What it shows:** nothing on its own — an empty full-screen box with the app
  background color, waiting for a fragment.
- **Why it's set up this way:** keeping the shell empty and generic lets three different
  screens reuse the exact same file; each just drops in a different fragment. The
  background color avoids a flash of white before the fragment appears.
- **How the code uses it:** each hosting Activity calls
  `setContentView(R.layout.activity_fragment_host)` and then, on first launch, runs a
  fragment transaction like `replace(R.id.fragment_host, new SearchFragment())` (and
  similarly `ProfileSettingsFragment` / `MyItemsFragment`).
