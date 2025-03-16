package com.example.money;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.money.databinding.ActivityMainBinding;
import com.example.money.services.SyncService;
import com.example.money.ui.create_news.CreateNewsFragment;
import com.example.money.ui.home.HomeFragment;
import com.example.money.ui.slideshow.SlideshowFragment;
import com.example.money.utils.NetworkUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Запуск синхронизации при входе в приложение
        if (NetworkUtils.isNetworkAvailable(this)) {
            SyncService.startSync(this);
        }

        setupFragments();
        setupBottomNavigation();

        // Находим FAB
        FloatingActionButton logoutButton = findViewById(R.id.logoutButton);

        // Обработка нажатия на FAB
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Удаляем состояние авторизации
                SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("is_logged_in", false);
                editor.apply();

                // Переходим на LoginActivity
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        if (savedInstanceState == null) {
            loadFragment(new CreateNewsFragment());
        }
    }

    private void setupFragments() {
        fragmentMap.put(R.id.nav_create_news, new CreateNewsFragment());
        fragmentMap.put(R.id.nav_gallery, new HomeFragment());
        fragmentMap.put(R.id.nav_slideshow, new SlideshowFragment());
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = fragmentMap.get(item.getItemId());
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}