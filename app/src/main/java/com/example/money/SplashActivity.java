package com.example.money;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity); // Создайте layout-файл splash_activity.xml

        // Задержка для имитации загрузки (например, 2 секунды)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAuthStatus();
            }
        }, 2000);
    }

    private void checkAuthStatus() {
        // Получаем SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false);

        if (isLoggedIn) {
            // Если пользователь авторизован, переходим на MainActivity
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            // Если пользователь не авторизован, переходим на LoginActivity
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
        }

        finish(); // Закрываем SplashActivity
    }
}
