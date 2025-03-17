package com.example.money.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.money.MainActivity;
import com.example.money.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Button changeThemeButton = binding.buttonChangeTheme;

        // Устанавливаем текст кнопки в зависимости от текущей темы
        updateButtonText(changeThemeButton);

        changeThemeButton.setOnClickListener(v -> changeTheme());

        return root;
    }

    private void changeTheme() {
        // Получаем текущую тему
        int currentThemeMode = AppCompatDelegate.getDefaultNightMode();

        // Определяем новую тему
        int newThemeMode;
        if (currentThemeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            newThemeMode = AppCompatDelegate.MODE_NIGHT_NO; // Переключаем на светлую тему
        } else {
            newThemeMode = AppCompatDelegate.MODE_NIGHT_YES; // Переключаем на темную тему
        }

        // Сохраняем новую тему
        saveTheme(newThemeMode);

        // Применяем новую тему
        AppCompatDelegate.setDefaultNightMode(newThemeMode);

        // Перезапуск активности для применения изменений с анимацией
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        getActivity().finish();
    }

    private void saveTheme(int themeMode) {
        SharedPreferences preferences = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("theme", themeMode);
        editor.apply();
    }

    private void updateButtonText(Button button) {
        int currentThemeMode = AppCompatDelegate.getDefaultNightMode();
        if (currentThemeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            button.setText("Включить светлую тему");
        } else {
            button.setText("Включить темную тему");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}