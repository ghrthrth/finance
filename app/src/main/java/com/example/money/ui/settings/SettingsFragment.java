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
import com.example.money.R;
import com.example.money.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    public static final String THEME_PREFERENCE = "theme_preference";
    public static final String THEME_KEY = "current_theme";

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
        SharedPreferences preferences = getActivity().getSharedPreferences(THEME_PREFERENCE, Context.MODE_PRIVATE);
        String currentTheme = preferences.getString(THEME_KEY, "Default");

        String newTheme;
        if (currentTheme.equals("Default")) {
            newTheme = "Green";
        } else {
            newTheme = "Default";
        }

        saveTheme(newTheme);
        applyTheme(newTheme);

        // Перезапуск всех активностей
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        getActivity().finish();
    }

    private void saveTheme(String theme) {
        SharedPreferences preferences = getActivity().getSharedPreferences(THEME_PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(THEME_KEY, theme);
        editor.apply();
    }

    private void applyTheme(String theme) {
        SharedPreferences preferences = getActivity().getSharedPreferences(THEME_PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        switch (theme) {
            case "Green":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Светлая тема
                editor.putString(THEME_KEY, "Green");
                editor.apply();
                getActivity().setTheme(R.style.AppTheme_Green); // Применяем кастомную тему
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Светлая тема
                editor.putString(THEME_KEY, "Default");
                editor.apply();
                getActivity().setTheme(R.style.Theme_Money); // Применяем стандартную тему
                break;
        }
    }

    private void updateButtonText(Button button) {
        SharedPreferences preferences = getActivity().getSharedPreferences(THEME_PREFERENCE, Context.MODE_PRIVATE);
        String currentTheme = preferences.getString(THEME_KEY, "Default");

        if (currentTheme.equals("Green")) {
            button.setText("Включить стандартную тему");
        } else {
            button.setText("Включить зеленую тему");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}