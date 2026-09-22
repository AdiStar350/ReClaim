# SearchActivity.java

## Overview
This is a thin host **Activity** for search (see `docs/GLOSSARY.md`). It only shows a fragment-host layout and places `SearchFragment` inside. Filters, list/map toggle, and API calls live in the Fragment.

## Concepts used here
- **Activity** — full screen hosting a Fragment. See `docs/GLOSSARY.md`.
- **Fragment** — `SearchFragment` holds the search UI. See `docs/GLOSSARY.md`.
- **Fragment transaction** — `replace` attaches the Fragment to the container.

## Libraries and imports
- **AndroidX / AppCompat**: `AppCompatActivity`, `Fragment`, nullability annotations.
- **Project**: `R.layout.activity_fragment_host`, `R.id.fragment_host`, and `SearchFragment`.

## Functions

### `protected void onCreate(Bundle savedInstanceState)`
```java
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_fragment_host);

    if (savedInstanceState == null) {
        Fragment fragment = new SearchFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_host, fragment)
                .commit();
    }
}
```
- **What it does:** Inflates the host layout and, on a fresh create, puts `SearchFragment` into `fragment_host`.
- **Where it's called:** By Android when this Activity starts (declared in `AndroidManifest.xml`).
- **Why it's written this way:** Matches the other standalone host Activities so Search can also open outside the dashboard bottom nav if needed. The null `savedInstanceState` check prevents a duplicate Fragment after rotation.
- **Step by step:**
  1. Call `super.onCreate`.
  2. Set content to `activity_fragment_host`.
  3. If not restoring state, create `SearchFragment`, replace into `R.id.fragment_host`, commit.
