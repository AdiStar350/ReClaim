# MyItemsActivity.java

## Overview
This is a thin host **Activity** for the "My items" tab content (see `docs/GLOSSARY.md`). It only loads a simple fragment-host layout and places a `MyItemsFragment` inside it. The real list UI lives in the Fragment.

## Concepts used here
- **Activity** — one full screen; here it only hosts a Fragment. See `docs/GLOSSARY.md`.
- **Fragment** — reusable screen piece; `MyItemsFragment` holds the actual "my items" UI. See `docs/GLOSSARY.md`.
- **Fragment transaction** — `beginTransaction().replace(...).commit()` swaps the Fragment into the host container.

## Libraries and imports
- **AndroidX / AppCompat**: `AppCompatActivity` base class; `Fragment` type; annotations for nullability.
- **Project**: `R.layout.activity_fragment_host` and `R.id.fragment_host` for the empty host layout; `MyItemsFragment` is the content.

## Functions

### `protected void onCreate(Bundle savedInstanceState)`
```java
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_fragment_host);

    if (savedInstanceState == null) {
        Fragment fragment = new MyItemsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_host, fragment)
                .commit();
    }
}
```
- **What it does:** Sets the host layout and, on a fresh create, puts `MyItemsFragment` into `fragment_host`.
- **Where it's called:** By Android when this Activity starts. It is registered in `AndroidManifest.xml`. `AppNotifications` builds an Intent to this Activity (for a notification tap that opens "My items").
- **Why it's written this way:** The `savedInstanceState == null` check avoids adding a second Fragment after rotation; the FragmentManager restores the old one.
- **Step by step:**
  1. Call `super.onCreate`.
  2. Inflate `activity_fragment_host`.
  3. If this is not a restore, create `MyItemsFragment`, replace it into `R.id.fragment_host`, and commit.
