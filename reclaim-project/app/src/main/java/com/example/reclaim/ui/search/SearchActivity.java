package com.example.reclaim.ui.search;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.reclaim.R;

/**
 * Standalone search activity hosting {@link SearchFragment}.
 */
public class SearchActivity extends AppCompatActivity {

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
}
