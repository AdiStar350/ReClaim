package com.example.reclaim.ui.dashboard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.reclaim.R;
import com.example.reclaim.databinding.ActivityDashboardBinding;
import com.example.reclaim.notifications.FcmTokenRegistrar;
import com.example.reclaim.sync.NetworkMonitor;
import com.example.reclaim.ui.myitems.MyItemsFragment;
import com.example.reclaim.ui.profile.ProfileSettingsFragment;
import com.example.reclaim.ui.report.ReportActivity;
import com.example.reclaim.ui.search.SearchFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Main shell with bottom navigation: Home, Search, My Items, Profile.
 */
public class DashboardActivity extends AppCompatActivity implements DashboardNavigator {

    /** Extra from a CLAIM_PENDING notification: land on the home tab. */
    public static final String EXTRA_FOCUS_HOME = "FOCUS_HOME";

    private static final String TAG_HOME = "home";
    private static final String TAG_SEARCH = "search";
    private static final String TAG_MY_ITEMS = "my_items";
    private static final String TAG_PROFILE = "profile";

    private ActivityDashboardBinding binding;

    /** Asks for POST_NOTIFICATIONS on Android 13+; pushes are silent if denied. */
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> { /* no-op: system respects the user's choice */ });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets();
        requestNotificationPermissionIfNeeded();
        FcmTokenRegistrar.register(this);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });

        // Only seed the first tab on a fresh start. After a config change the
        // FragmentManager restores the current fragment itself (and recreating
        // it re-runs its full lifecycle, so data is refreshed anyway).
        if (savedInstanceState == null) {
            showTab(R.id.nav_home);
            showOfflineDialogIfNeeded();
        }
        handleHomeFocus(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleHomeFocus(intent);
    }

    /**
     * Claim-related notifications always land on the home tab so the
     * incoming-claim or decision card is visible immediately.
     */
    private void handleHomeFocus(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_FOCUS_HOME, false)) {
            intent.removeExtra(EXTRA_FOCUS_HOME);
            binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
            showTab(R.id.nav_home);
        }
    }

    /**
     * When the app is opened with no active connection, shows a single blocking
     * dialog explaining that reports can still be created offline. This replaces
     * the stream of per-screen connection-error toasts.
     */
    private void showOfflineDialogIfNeeded() {
        if (NetworkMonitor.isOnline(this)) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setMessage(R.string.offline_dialog_message)
                .setPositiveButton(R.string.offline_dialog_continue, (dialog, which) ->
                        startActivity(new Intent(this, ReportActivity.class)))
                .setNegativeButton(R.string.offline_dialog_exit, (dialog, which) ->
                        finishAffinity())
                .show();
    }

    /**
     * Applies system bar insets manually: the bottom navigation is padded so it
     * rests fully above the system navigation area, and the fragment container
     * is kept below the status bar and above the (now taller) bottom nav.
     */
    private void applyWindowInsets() {
        int baseNavHeight = getResources().getDimensionPixelSize(R.dimen.bottom_nav_height);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Lift the nav items above the system navigation bar; the dark
            // background still fills the inset strip behind the system UI.
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);

            // Keep fragment content below the status bar.
            binding.fragmentContainer.setPadding(0, systemBars.top, 0, 0);

            // Push content above the taller bottom nav.
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) binding.fragmentContainer.getLayoutParams();
            params.bottomMargin = baseNavHeight + systemBars.bottom;
            binding.fragmentContainer.setLayoutParams(params);

            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    public void selectTab(@IdRes int menuItemId) {
        binding.bottomNavigation.setSelectedItemId(menuItemId);
        showTab(menuItemId);
    }

    private void showTab(@IdRes int itemId) {
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

    /**
     * Swaps in a brand-new instance of the tab's fragment via {@code replace()}.
     * Unlike show/hide, this destroys the outgoing fragment and recreates the
     * incoming one from scratch, so its full lifecycle — including
     * {@code onCreateView}, {@code onViewCreated} and {@code onResume} — runs
     * every time the tab is selected, guaranteeing a fresh data fetch / state
     * initialization instead of a remembered state.
     */
    private void showFragment(@NonNull String tag, @NonNull FragmentFactory factory) {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, factory.create(), tag)
                .commit();
    }

    @FunctionalInterface
    private interface FragmentFactory {
        Fragment create();
    }
}
