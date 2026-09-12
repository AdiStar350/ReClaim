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
import com.example.reclaim.sync.OfflineProtocol;
import com.example.reclaim.ui.myitems.MyItemsFragment;
import com.example.reclaim.ui.report.ReportActivity;
import com.example.reclaim.ui.profile.ProfileSettingsFragment;
import com.example.reclaim.ui.search.SearchFragment;

/**
 * Main screen with Home, Search, My Items, and Profile.
 */
public class DashboardActivity extends AppCompatActivity implements DashboardNavigator {

    /** From a claim notification: open the home tab. */
    public static final String EXTRA_FOCUS_HOME = "FOCUS_HOME";

    private static final String TAG_HOME = "home";
    private static final String TAG_SEARCH = "search";
    private static final String TAG_MY_ITEMS = "my_items";
    private static final String TAG_PROFILE = "profile";

    private ActivityDashboardBinding binding;

    /** Asks to show notifications on Android 13+. Silent if the user says no. */
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> { /* nothing to do; the system already applied the choice */ });

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleHomeFocus(intent);
    }

    /**
     * Claim notifications open the home tab so the card is visible.
     */
    private void handleHomeFocus(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_FOCUS_HOME, false)) {
            intent.removeExtra(EXTRA_FOCUS_HOME);
            binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
            showTab(R.id.nav_home);
        }
    }

    /**
     * Pads the bottom bar and the content so they sit clear of the system bars.
     */
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

    /**
     * Replaces the current tab with a new one so the screen loads fresh each time.
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
