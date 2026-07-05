package com.example.reclaim.ui.profile;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.reclaim.R;

/**
 * Standalone profile & settings activity hosting {@link ProfileSettingsFragment}.
 */
public class ProfileActivity extends AppCompatActivity {

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
}
