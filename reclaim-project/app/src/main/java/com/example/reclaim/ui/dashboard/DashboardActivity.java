package com.example.reclaim.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.reclaim.R;
import com.example.reclaim.databinding.ActivityDashboardBinding;
import com.example.reclaim.notifications.FcmTokenRegistrar;
import com.example.reclaim.ui.myitems.MyItemsFragment;
import com.example.reclaim.ui.profile.ProfileSettingsFragment;
import com.example.reclaim.ui.search.SearchFragment;

/**
 * Main shell with bottom navigation: Home, Search, My Items, Profile.
 */
public class DashboardActivity extends AppCompatActivity implements DashboardNavigator {

    private static final String TAG_HOME = "home";
    private static final String TAG_SEARCH = "search";
    private static final String TAG_MY_ITEMS = "my_items";
    private static final String TAG_PROFILE = "profile";

    private ActivityDashboardBinding binding;
    @Nullable
    private Fragment activeFragment;

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

        if (savedInstanceState == null) {
            showTab(R.id.nav_home);
        } else {
            restoreActiveFragment();
        }
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

    private void showFragment(@NonNull String tag, @NonNull FragmentFactory factory) {
        Fragment target = getSupportFragmentManager().findFragmentByTag(tag);
        var transaction = getSupportFragmentManager().beginTransaction();

        if (target == null) {
            target = factory.create();
            transaction.add(R.id.fragment_container, target, tag);
        } else {
            transaction.show(target);
        }

        if (activeFragment != null && activeFragment != target) {
            transaction.hide(activeFragment);
        }

        transaction.commit();
        activeFragment = target;
    }

    private void restoreActiveFragment() {
        int selectedId = binding.bottomNavigation.getSelectedItemId();
        if (selectedId == R.id.nav_search) {
            activeFragment = getSupportFragmentManager().findFragmentByTag(TAG_SEARCH);
        } else if (selectedId == R.id.nav_my_items) {
            activeFragment = getSupportFragmentManager().findFragmentByTag(TAG_MY_ITEMS);
        } else if (selectedId == R.id.nav_profile) {
            activeFragment = getSupportFragmentManager().findFragmentByTag(TAG_PROFILE);
        } else {
            activeFragment = getSupportFragmentManager().findFragmentByTag(TAG_HOME);
        }

        if (activeFragment == null) {
            showTab(R.id.nav_home);
        }
    }

    @FunctionalInterface
    private interface FragmentFactory {
        Fragment create();
    }
}
