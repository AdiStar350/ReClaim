package com.example.reclaim.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.reclaim.R;
import com.example.reclaim.databinding.ActivityDashboardBinding;
import com.example.reclaim.ui.profile.ProfileFragment;
import com.example.reclaim.ui.report.ReportActivity;

/**
 * Primary dashboard screen of the ReClaim application.
 * <p>
 * Hosts a {@link com.google.android.material.bottomnavigation.BottomNavigationView}
 * that switches between {@link ListFragment}, {@link MapFragment}, and
 * {@link ProfileFragment}. A Floating Action Button opens
 * {@link ReportActivity} to let users file a new lost/found report.
 * </p>
 */
public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBottomNavigation();
        setupFab();

        // Load default fragment only on fresh creation (not on config change)
        if (savedInstanceState == null) {
            loadFragment(new ListFragment());
        }
    }

    /**
     * Configures the bottom navigation bar to switch between the three
     * primary fragments.
     */
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new ListFragment();
            } else if (itemId == R.id.nav_map) {
                selectedFragment = new MapFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    /**
     * Configures the Floating Action Button to navigate to the report screen.
     */
    private void setupFab() {
        binding.fabReport.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ReportActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Replaces the current fragment in the container with the given fragment.
     *
     * @param fragment the fragment to display
     */
    private void loadFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
