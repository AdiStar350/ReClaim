package com.example.reclaim.ui.myitems;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.reclaim.R;

/**
 * Standalone my-items activity hosting {@link MyItemsFragment}.
 */
public class MyItemsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_host);

        if (savedInstanceState == null) {
            Fragment fragment = new MyItemsFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_host, fragment)
                    .commit();
        }
    }
}
