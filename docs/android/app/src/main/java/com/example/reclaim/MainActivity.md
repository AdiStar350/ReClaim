# MainActivity.java

## Overview
This is the first **Activity** the app opens (see `docs/GLOSSARY.md`). It does not draw a UI. It checks whether a login **JWT** is already saved on the phone and immediately sends the user either to the home dashboard or to the sign-in screen, then closes itself.

## Concepts used here
- **Activity** — one full screen with a lifecycle; here it only runs `onCreate` and leaves. See `docs/GLOSSARY.md`.
- **Intent** — a message that starts another Activity. See `docs/GLOSSARY.md`.
- **SharedPreferences (via `TokenManager`)** — the saved JWT is read to decide where to go. See `docs/GLOSSARY.md`.
- **JWT** — the login token that proves the user is signed in. See `docs/GLOSSARY.md`.

## Libraries and imports
- **AndroidX / AppCompat**: `AppCompatActivity` is the base class for screens.
- **Android framework**: `Intent` opens the next screen; `Bundle` is the usual `onCreate` parameter (unused here beyond the superclass call).
- **Project classes**: `TokenManager` checks for a saved token; `LoginActivity` and `DashboardActivity` are the two possible destinations.

## Functions

### `protected void onCreate(Bundle savedInstanceState)`
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent;
    if (TokenManager.hasToken(this)) {
        intent = new Intent(this, DashboardActivity.class);
    } else {
        intent = new Intent(this, LoginActivity.class);
    }

    startActivity(intent);
    finish();
}
```
- **What it does:** Picks Dashboard or Login based on whether a token exists, starts that screen, and finishes so this launcher screen is gone.
- **Where it's called:** By the Android system when the app launches `MainActivity` (it is the launcher Activity in `AndroidManifest.xml`). `OfflineProtocol` also special-cases this Activity type when deciding how to handle offline prompts.
- **Why it's written this way:** Keeping the router tiny means there is no flash of an empty layout. `finish()` removes it from the back stack so Back from Login/Dashboard does not return here.
- **Step by step:**
  1. Call `super.onCreate`.
  2. If `TokenManager.hasToken(this)` is true, build an Intent to `DashboardActivity`; otherwise to `LoginActivity`.
  3. `startActivity` that Intent.
  4. `finish()` this Activity.
