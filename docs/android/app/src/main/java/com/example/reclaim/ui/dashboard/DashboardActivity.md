# DashboardActivity.java

## Overview
This is the main screen after login. An **Activity** is one full screen with its own logic and lifecycle (see `docs/GLOSSARY.md`). It holds a bottom navigation bar with four tabs — Home, Search, My Items, Profile — plus a center "add report" button, and swaps in the matching **Fragment** for whichever tab is selected. It also asks for notification permission on newer Android, registers the push token, and handles being opened from a claim notification. It implements `DashboardNavigator` so a Fragment can ask it to switch tabs.

## Concepts used here
- **Activity lifecycle** — `onCreate` builds the screen; `onNewIntent` handles being re-opened. See `docs/GLOSSARY.md`.
- **Fragment + FragmentManager transaction** — each tab is a Fragment swapped into a container. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityDashboardBinding` exposes the views from `activity_dashboard.xml`. See `docs/GLOSSARY.md`.
- **Intent + extras** — used to open the report screen and to detect the "focus home" flag from a notification. See `docs/GLOSSARY.md`.
- **Activity Result API** — requests the notifications permission and receives the user's answer.
- **Window insets** — code that pads the UI so it does not sit under the status/navigation bars.
- **Interface implementation** — implements `DashboardNavigator.selectTab`. See `docs/GLOSSARY.md`.

## Libraries and imports
- **AndroidX / AppCompat**: `AppCompatActivity` base class; `ActivityResultLauncher`/`ActivityResultContracts.RequestPermission` for the notification prompt; `ContextCompat.checkSelfPermission` to check permission; `ViewCompat`/`WindowInsetsCompat`/`Insets` to handle system-bar spacing; `Fragment` is the tab content type; `@IdRes`/`@NonNull` are annotations.
- **Android framework**: `Intent` opens the report screen and carries the notification flag; `Build`/`Manifest` gate the Android-13 notification permission; `PackageManager` reports permission state; `ViewGroup.MarginLayoutParams` adjusts margins.
- **Project classes**: `ActivityDashboardBinding` (layout); `FcmTokenRegistrar` (registers the push token); `OfflineProtocol` (shows the offline notice when needed); the four tab Fragments `HomeFragment`, `SearchFragment`, `MyItemsFragment`, `ProfileSettingsFragment`; and `ReportActivity` (opened by the add button).

## Fields
- `public static final String EXTRA_FOCUS_HOME = "FOCUS_HOME";` — an intent-extra key; a claim notification sets it so the dashboard opens the Home tab.
- `private static final String TAG_HOME / TAG_SEARCH / TAG_MY_ITEMS / TAG_PROFILE;` — tags used to identify each tab's Fragment in the FragmentManager.
- `private ActivityDashboardBinding binding;` — typed access to the views (nav bar, container, add button).
- `private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(...)` — asks for the POST_NOTIFICATIONS permission on Android 13+; its callback does nothing because the system already applied the choice. It is a field because the Activity Result API requires registration before the screen starts.

## Functions

### `private void handleHomeFocus(Intent intent)`
```java
private void handleHomeFocus(Intent intent) {
    if (intent != null && intent.getBooleanExtra(EXTRA_FOCUS_HOME, false)) {
        intent.removeExtra(EXTRA_FOCUS_HOME);
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        showTab(R.id.nav_home);
    }
}
```
- **What it does:** If the screen was opened (or re-opened) by a claim notification asking to focus Home, it selects and shows the Home tab.
- **Where it's called:** From `onCreate` and from `onNewIntent`.
- **Why it's written this way:** It removes the extra after handling so the same jump does not repeat on the next rotation or re-entry.
- **Step by step:**
  1. If the intent carries `EXTRA_FOCUS_HOME` set to true, remove that extra.
  2. Set the bottom bar's selected item to Home and call `showTab(R.id.nav_home)`.

### `private void applyWindowInsets()`
```java
private void applyWindowInsets() {
    int baseNavHeight = getResources().getDimensionPixelSize(R.dimen.bottom_nav_height);

    ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
        Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

        binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);

        binding.fragmentContainer.setPadding(0, systemBars.top, 0, 0);

        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) binding.fragmentContainer.getLayoutParams();
        params.bottomMargin = baseNavHeight + systemBars.bottom;
        binding.fragmentContainer.setLayoutParams(params);

        ViewGroup.MarginLayoutParams fabParams =
                (ViewGroup.MarginLayoutParams) binding.fabAddReport.getLayoutParams();
        fabParams.bottomMargin = getResources()
                .getDimensionPixelSize(R.dimen.space_40) + systemBars.bottom;
        binding.fabAddReport.setLayoutParams(fabParams);

        return WindowInsetsCompat.CONSUMED;
    });
}
```
- **What it does:** Adjusts padding and margins so the nav bar, the content area, and the add button all sit clear of the phone's status and navigation bars.
- **Where it's called:** Once from `onCreate`.
- **Why it's written this way:** The screen draws edge-to-edge, so the code reads the system-bar insets and pushes content out of their way; the nav bar keeps its dark fill behind the bar while its items stay above it.
- **Step by step:**
  1. Read the base nav-bar height from resources.
  2. Register an insets listener on the root view.
  3. When insets arrive, pad the bottom nav by the bottom inset and the content by the top inset.
  4. Set the content's bottom margin to the nav height plus the bottom inset.
  5. Raise the add button by a fixed spacing plus the bottom inset.
  6. Return `CONSUMED` so the insets are not applied again by children.

### `private void requestNotificationPermissionIfNeeded()`
```java
private void requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return;
    }
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
}
```
- **What it does:** On Android 13+ (Tiramisu), asks for permission to post notifications if it has not been granted yet.
- **Where it's called:** Once from `onCreate`.
- **Why it's written this way:** The notification permission only exists from Android 13, so older versions return early; asking only when not already granted avoids a needless prompt.
- **Step by step:**
  1. If the OS is older than Android 13, do nothing.
  2. Otherwise, if notifications are not already granted, launch the permission request.

### `private void showTab(int itemId)`
```java
private void showTab(@IdRes int itemId) {
    if (itemId == R.id.nav_report_placeholder) {
        return;
    }
    if (itemId == R.id.nav_search) {
        showFragment(TAG_SEARCH, SearchFragment::new);
    } else if (itemId == R.id.nav_my_items) {
        showFragment(TAG_MY_ITEMS, MyItemsFragment::new);
    } else if (itemId == R.id.nav_profile) {
        showFragment(TAG_PROFILE, ProfileSettingsFragment::new);
    } else {
        showFragment(TAG_HOME, HomeFragment::new);
    }
}
```
- **What it does:** Maps a bottom-nav menu id to the correct Fragment and shows it.
- **Where it's called:** From `selectTab`, `handleHomeFocus`, `onCreate` (fresh start), and the bottom-nav item-selected listener.
- **Why it's written this way:** The center "report" slot is only a placeholder (the add button handles reports), so its id is ignored. Each real tab maps to a Fragment factory (`Type::new`) passed to `showFragment`.
- **Step by step:**
  1. Ignore the report placeholder id.
  2. For Search/My Items/Profile, show the matching Fragment with its tag.
  3. For anything else (Home), show `HomeFragment`.

### `private void showFragment(String tag, FragmentFactory factory)`
```java
private void showFragment(@NonNull String tag, @NonNull FragmentFactory factory) {
    getSupportFragmentManager().beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, factory.create(), tag)
            .commit();
}
```
- **What it does:** Replaces whatever is in the fragment container with a freshly created Fragment.
- **Where it's called:** From `showTab`.
- **Why it's written this way:** Using `replace` with a new instance each time means every tab loads fresh (re-reading data on `onResume`), which keeps the tabs simple and always up to date. `setReorderingAllowed(true)` is the recommended, more efficient transaction mode.
- **Step by step:**
  1. Begin a fragment transaction.
  2. Enable reordering.
  3. Replace the container's content with `factory.create()` under the given tag.
  4. Commit the transaction.

### `public void selectTab(int menuItemId)`
```java
@Override
public void selectTab(@IdRes int menuItemId) {
    binding.bottomNavigation.setSelectedItemId(menuItemId);
    showTab(menuItemId);
}
```
- **What it does:** Selects a bottom-nav tab and shows its Fragment; this is the implementation of `DashboardNavigator`.
- **Where it's called:** By a Fragment through the `DashboardNavigator` interface — for example `HomeFragment`'s "show more" button calls `navigator.selectTab(R.id.nav_search)`.
- **Why it's written this way:** Exposing this through the interface lets Fragments switch tabs without referencing `DashboardActivity` directly.
- **Step by step:**
  1. Highlight the requested item in the bottom bar.
  2. Show that tab's Fragment via `showTab`.

### `protected void onNewIntent(Intent intent)`
```java
@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleHomeFocus(intent);
}
```
- **What it does:** Handles the dashboard being re-opened (while already running) with a new intent, such as a tap on a claim notification.
- **Where it's called:** Android calls it when a new `Intent` is delivered to the already-running Activity.
- **Why it's written this way:** It stores the new intent with `setIntent` and re-checks the "focus home" flag so a notification tap jumps to Home even if the dashboard was already open.
- **Step by step:**
  1. Call `super.onNewIntent`.
  2. Save the new intent.
  3. Run `handleHomeFocus` on it.

### `protected void onCreate(Bundle savedInstanceState)`
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = ActivityDashboardBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    applyWindowInsets();
    requestNotificationPermissionIfNeeded();
    FcmTokenRegistrar.register(this);

    binding.fabAddReport.setOnClickListener(v ->
            startActivity(new Intent(this, ReportActivity.class)));

    binding.bottomNavigation.setOnItemSelectedListener(item -> {
        if (item.getItemId() == R.id.nav_report_placeholder) {
            return false;
        }
        showTab(item.getItemId());
        return true;
    });

    // Open home only on a fresh start. After rotation the current tab comes back on its own.
    if (savedInstanceState == null) {
        showTab(R.id.nav_home);
        OfflineProtocol.showIfNeeded(this);
    }
    handleHomeFocus(getIntent());
}
```
- **What it does:** Builds the dashboard: sets up spacing, permissions, the push token, the add button, tab switching, the initial tab, and any notification focus.
- **Where it's called:** Android calls it when the Activity is created.
- **Why it's written this way:** It only forces the Home tab and the offline check on a *fresh* start (`savedInstanceState == null`); after a rotation the previously selected tab is restored automatically, so it must not be reset. Returning `false` for the report placeholder keeps it from being "selected" like a real tab.
- **Step by step:**
  1. Inflate `activity_dashboard.xml` and set it as content.
  2. Apply window insets, request notification permission if needed, and register the push token.
  3. Wire the add button to open `ReportActivity`.
  4. Wire the bottom-nav listener: ignore the placeholder (return false), otherwise show the chosen tab (return true).
  5. On a fresh start only, show Home and run the offline check.
  6. Run `handleHomeFocus` on the launching intent.

### `interface FragmentFactory` (inner functional interface)
```java
@FunctionalInterface
private interface FragmentFactory {
    Fragment create();
}
```
- **What it does:** A tiny one-method interface describing "something that can make a Fragment", so tab constructors can be passed as `HomeFragment::new`, `SearchFragment::new`, etc.
- **Where it's called:** Used as the second parameter of `showFragment`; the factories are supplied in `showTab`.
- **Why it's written this way:** Marking it `@FunctionalInterface` lets a method reference (`SomeFragment::new`) satisfy it, so the fragment is only created when the tab is actually shown (lazy creation).
- **Step by step:**
  1. Declares one method `create()` that returns a new `Fragment`.
