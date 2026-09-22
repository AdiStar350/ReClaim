# ProfileActivity.java

## Overview
This is a thin host **Activity** for the profile / settings content (see `docs/GLOSSARY.md`). It loads a fragment-host layout and places `ProfileSettingsFragment` inside. The buttons and API calls live in the Fragment.

## Concepts used here
- **Activity** — full screen that only hosts a Fragment here. See `docs/GLOSSARY.md`.
- **Fragment** — `ProfileSettingsFragment` is the real profile UI. See `docs/GLOSSARY.md`.
- **Fragment transaction** — `replace` puts the Fragment into the host container.

## Libraries and imports
- **AndroidX / AppCompat**: `AppCompatActivity`, `Fragment`, nullability annotations.
- **Project**: `R.layout.activity_fragment_host`, `R.id.fragment_host`, and `ProfileSettingsFragment`.

## Functions

### `protected void onCreate(Bundle savedInstanceState)`
```java
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_fragment_host);

    if (savedInstanceState == null) {
        Fragment fragment = new ProfileSettingsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_host, fragment)
                .commit();
    }
}
```
- **What it does:** Sets the host layout and, on first create, inserts `ProfileSettingsFragment`.
- **Where it's called:** By Android when this Activity starts (declared in `AndroidManifest.xml`).
- **Why it's written this way:** Same host pattern as Search/My Items — keep Activity thin and put logic in the Fragment. Skip adding a Fragment again after rotation.
- **Step by step:**
  1. Call `super.onCreate`.
  2. Set `activity_fragment_host` as content.
  3. If `savedInstanceState` is null, create `ProfileSettingsFragment`, replace into `fragment_host`, commit.
