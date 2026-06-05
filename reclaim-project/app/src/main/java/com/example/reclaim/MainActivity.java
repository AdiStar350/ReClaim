package com.example.reclaim;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.reclaim.network.TokenManager;
import com.example.reclaim.ui.auth.LoginActivity;
import com.example.reclaim.ui.dashboard.DashboardActivity;

/**
 * Main entry point for the ReClaim application.
 * <p>
 * Routes authenticated users to {@link DashboardActivity} and others to
 * {@link LoginActivity}.
 * </p>
 */
public class MainActivity extends AppCompatActivity {

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
}
