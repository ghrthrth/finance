package com.example.money.ui.category;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.money.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategorySelectionFragment extends Fragment {

    private OnCategorySelectedListener listener;
    private List<String> categories;
    private ArrayAdapter<String> adapter;
    private SharedPreferences sharedPreferences;

    // Интерфейс для передачи выбранной категории
    public interface OnCategorySelectedListener {
        void onCategorySelected(String category);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Получаем целевой фрагмент (HomeFragment)
        if (getTargetFragment() instanceof OnCategorySelectedListener) {
            listener = (OnCategorySelectedListener) getTargetFragment();
        } else {
            throw new RuntimeException(context.toString() + " must implement OnCategorySelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_selection, container, false);

        // Инициализация SharedPreferences для хранения категорий
        sharedPreferences = requireContext().getSharedPreferences("categories", Context.MODE_PRIVATE);

        // Инициализация GridView
        GridView gridView = view.findViewById(R.id.gridViewCategories);

        // Загрузка категорий из SharedPreferences
        categories = new ArrayList<>(loadCategories());

        // Адаптер для GridView
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categories);
        gridView.setAdapter(adapter);

        // Обработчик выбора категории
        gridView.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedCategory = categories.get(position);
            if (listener != null) {
                listener.onCategorySelected(selectedCategory); // Передаем выбранную категорию
            }
            // Закрываем фрагмент
            getParentFragmentManager().popBackStack();
        });

        // Кнопка для добавления новой категории
        Button btnAddCategory = view.findViewById(R.id.btnAddCategory);
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        return view;
    }

    // Диалог для добавления новой категории
    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Добавить категорию");

        // Поле для ввода названия категории
        final EditText input = new EditText(requireContext());
        builder.setView(input);

        // Кнопка "Добавить"
        builder.setPositiveButton("Добавить", (dialog, which) -> {
            String categoryName = input.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                categories.add(categoryName);
                saveCategories(categories); // Сохраняем категории
                adapter.notifyDataSetChanged(); // Обновляем GridView
            }
        });

        // Кнопка "Отмена"
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Сохранение категорий в SharedPreferences
    private void saveCategories(List<String> categories) {
        Set<String> categorySet = new HashSet<>(categories);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("category_list", categorySet);
        editor.apply();
    }

    // Загрузка категорий из SharedPreferences
    private Set<String> loadCategories() {
        Set<String> defaultCategories = new HashSet<>(Arrays.asList("Еда", "Транспорт", "Развлечения", "Жилье", "Здоровье", "Одежда"));
        return sharedPreferences.getStringSet("category_list", defaultCategories);
    }
}