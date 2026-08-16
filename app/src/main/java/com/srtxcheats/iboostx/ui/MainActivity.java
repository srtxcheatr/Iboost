package com.srtxcheats.iboostx.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.srtxcheats.iboostx.R;
import com.srtxcheats.iboostx.games.GamesFragment;
import com.srtxcheats.iboostx.settings.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private NebulaBackgroundView nebulaBackground;
    private final HomeFragment homeFragment = new HomeFragment();
    private final PerformanceFragment performanceFragment = new PerformanceFragment();
    private final GamesFragment gamesFragment = new GamesFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        setContentView(R.layout.activity_main);

        nebulaBackground = findViewById(R.id.nebulaBackground);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);

        if (savedInstanceState == null) {
            showFragment(homeFragment);
        }
    }

    private boolean onNavItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            showFragment(homeFragment);
            return true;
        } else if (id == R.id.nav_performance) {
            showFragment(performanceFragment);
            return true;
        } else if (id == R.id.nav_games) {
            showFragment(gamesFragment);
            return true;
        } else if (id == R.id.nav_suzuki) {
            startActivity(new android.content.Intent(this, SuzukiSetupActivity.class));
            return false; // don't select the tab; it's a wizard, not a persistent page
        } else if (id == R.id.nav_settings) {
            showFragment(settingsFragment);
            return true;
        }
        return false;
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragmentContainer, fragment);
        tx.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nebulaBackground != null) nebulaBackground.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nebulaBackground != null) nebulaBackground.onPause();
    }
}
