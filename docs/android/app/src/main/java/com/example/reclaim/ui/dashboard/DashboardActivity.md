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

What that function does: I show the tab host, the report FAB, and I ask for notification permission.
What the function is responsible for: I am the main signed-in screen.
Where it works: MainActivity, login, signup, and reset open me. I inflate activity_dashboard.xml. Claim notices use EXTRA_FOCUS_HOME.
Why I chose to write the code of the function the way its written: I skip recreating home after rotation. The middle nav item is a placeholder so the FAB can sit there.
More: I use ActivityDashboardBinding, FcmTokenRegistrar.register, and OfflineProtocol.showIfNeeded.

```java
@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleHomeFocus(intent);
}
```

What that function does: I take a new intent while I am already open and maybe switch to home.
What the function is responsible for: I handle a claim notification tap when the dashboard is already running.
Where it works: Android calls me for singleTop dashboard launches from AppNotifications.showClaimPending.
Why I chose to write the code of the function the way its written: setIntent keeps extras on getIntent for later.
More: I call handleHomeFocus.

```java
private void handleHomeFocus(Intent intent) {
    if (intent != null && intent.getBooleanExtra(EXTRA_FOCUS_HOME, false)) {
        intent.removeExtra(EXTRA_FOCUS_HOME);
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        showTab(R.id.nav_home);
    }
}
```

What that function does: I switch to the home tab when a claim notice asked for it.
What the function is responsible for: I make the pending-claim card visible after a push tap.
Where it works: I am called from onCreate and onNewIntent.
Why I chose to write the code of the function the way its written: I remove the extra so a later recreate does not keep forcing home.
More: I use EXTRA_FOCUS_HOME and showTab.

```java
private void applyWindowInsets() {
    int baseNavHeight = getResources().getDimensionPixelSize(R.dimen.bottom_nav_height);

    ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
        Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

        // Sit the nav items above the system bar. The dark fill still goes behind it.
        binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);

        // Keep content below the status bar.
        binding.fragmentContainer.setPadding(0, systemBars.top, 0, 0);

        // Leave room for the taller bottom bar.
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) binding.fragmentContainer.getLayoutParams();
        params.bottomMargin = baseNavHeight + systemBars.bottom;
        binding.fragmentContainer.setLayoutParams(params);

        // Keep the report button over the middle of the nav bar.
        ViewGroup.MarginLayoutParams fabParams =
                (ViewGroup.MarginLayoutParams) binding.fabAddReport.getLayoutParams();
        fabParams.bottomMargin = getResources()
                .getDimensionPixelSize(R.dimen.space_40) + systemBars.bottom;
        binding.fabAddReport.setLayoutParams(fabParams);

        return WindowInsetsCompat.CONSUMED;
    });
}
```

What that function does: I pad the nav bar, fragment, and FAB around the system bars.
What the function is responsible for: I keep content off the status bar and gesture bar.
Where it works: I am called from onCreate. The listener runs when insets change.
Why I chose to write the code of the function the way its written: Nav padding lets the bar color go behind the system bar while items sit above it. FAB margin tracks the same bottom inset.
More: I use ViewCompat.setOnApplyWindowInsetsListener and WindowInsetsCompat.Type.systemBars.

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

What that function does: I ask for POST_NOTIFICATIONS on Android 13+ if it is not granted.
What the function is responsible for: I am the one-time permission prompt for pushes.
Where it works: I am called from onCreate. The launcher ignores the result.
Why I chose to write the code of the function the way its written: Older Android does not need this permission. Saying no is allowed; I do not block the app.
More: I use ActivityResultContracts.RequestPermission and ContextCompat.checkSelfPermission.

```java
@Override
public void selectTab(@IdRes int menuItemId) {
    binding.bottomNavigation.setSelectedItemId(menuItemId);
    showTab(menuItemId);
}
```

What that function does: I select a bottom-nav item and show that tab.
What the function is responsible for: I implement DashboardNavigator for fragments.
Where it works: HomeFragment calls me when it wants to switch tabs.
Why I chose to write the code of the function the way its written: Updating the nav item keeps the highlight in sync with showTab.
More: I use BottomNavigationView.setSelectedItemId.

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

What that function does: I map a nav id to Home, Search, My Items, or Profile.
What the function is responsible for: I pick which fragment to put in the container.
Where it works: I am called from onCreate, the nav listener, selectTab, and handleHomeFocus.
Why I chose to write the code of the function the way its written: The report placeholder is ignored so the FAB handles report. Unknown ids fall back to home.
More: I use SearchFragment, MyItemsFragment, ProfileSettingsFragment, and HomeFragment constructors.

```java
private void showFragment(@NonNull String tag, @NonNull FragmentFactory factory) {
    getSupportFragmentManager().beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, factory.create(), tag)
            .commit();
}
```

What that function does: I replace the current tab with a new fragment instance.
What the function is responsible for: I keep each tab visit loading fresh data.
Where it works: I am called by showTab. The container is in activity_dashboard.xml.
Why I chose to write the code of the function the way its written: replace instead of hide/show so onCreateView runs again.
More: I use FragmentManager and FragmentFactory.create.

```java
Fragment create();
```

What that function does: I make the fragment for a tab.
What the function is responsible for: I am the factory method showFragment calls.
Where it works: Method references like HomeFragment::new implement me.
Why I chose to write the code of the function the way its written: A small interface lets showTab pass constructors without four overloads.
More: I am a @FunctionalInterface returning Fragment.
