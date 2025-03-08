package com.example.money.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.money.R;
import com.example.money.databinding.FragmentHomeBinding;
import com.example.money.ui.category.CategorySelectionFragment;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment implements CategorySelectionFragment.OnCategorySelectedListener {

    private FragmentHomeBinding binding;
    private PieChart pieChart;
    private RecyclerView recyclerView;
    private Button btnAddTransaction;
    private List<Transaction> transactions = new ArrayList<>();
    private TransactionAdapter adapter;

    private String selectedCategory; // Переменная для хранения выбранной категории
    private String amountStr = ""; // Переменная для хранения введенной суммы

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d("HomeFragment", "onCreateView called");

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Инициализация элементов интерфейса
        pieChart = binding.pieChart;
        btnAddTransaction = binding.btnAddTransaction;
        recyclerView = binding.recyclerView;

        // Настройка RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(transactions);
        recyclerView.setAdapter(adapter);

        pieChart.getDescription().setEnabled(false); // Отключение описания (label) диаграммы

        // Обработчик нажатия на кнопку
        btnAddTransaction.setOnClickListener(v -> addTransaction());

        updateChart(); // Обновление диаграммы

        pieChart.animateY(1000, Easing.EaseInOutQuad); // Плавная анимация с ускорением и замедлением

        return root;
    }

    private void addTransaction() {
        showBottomSheetDialog();
    }

    private void showBottomSheetDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_add_transaction, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        EditText etAmount = bottomSheetView.findViewById(R.id.etAmount);
        Button btnChooseCategory = bottomSheetView.findViewById(R.id.btnChooseCategory);
        Button btnSave = bottomSheetView.findViewById(R.id.btnSave);

        // Восстанавливаем ранее введенную сумму (если есть)
        if (!amountStr.isEmpty()) {
            etAmount.setText(amountStr);
        }

        // Восстанавливаем выбранную категорию (если есть)
        if (selectedCategory != null) {
            btnChooseCategory.setText(selectedCategory);
        }

        btnChooseCategory.setOnClickListener(v -> {
            // Сохраняем введенную сумму
            amountStr = etAmount.getText().toString();

            // Сворачиваем BottomSheetDialog
            bottomSheetDialog.dismiss();

            // Открываем фрагмент выбора категории
            CategorySelectionFragment categorySelectionFragment = new CategorySelectionFragment();
            categorySelectionFragment.setTargetFragment(this, 0); // Устанавливаем текущий фрагмент как целевой

            // Используем FragmentTransaction для отображения фрагмента
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, categorySelectionFragment) // Замените R.id.fragment_container на ваш контейнер для фрагментов
                    .addToBackStack(null) // Добавляем транзакцию в back stack
                    .commit();
        });

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (!amountStr.isEmpty() && selectedCategory != null) {
                int amount = Integer.parseInt(amountStr);
                // Добавляем транзакцию
                transactions.add(new Transaction.Builder()
                        .setCategory(selectedCategory)
                        .setAmount(amount)
                        .build());
                adapter.notifyDataSetChanged();
                updateChart();
                bottomSheetDialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Введите сумму и выберите категорию", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheetDialog.show();
    }

    @Override
    public void onCategorySelected(String category) {
        this.selectedCategory = category; // Сохраняем выбранную категорию
        Log.d("HomeFragment", "Выбрана категория: " + category);
        Toast.makeText(requireContext(), "Выбрана категория: " + category, Toast.LENGTH_SHORT).show();

        // Показываем BottomSheetDialog снова
        showBottomSheetDialog();
    }

    private void updateChart() {
        Map<String, Float> categoryAmountMap = new HashMap<>();

        // Собираем данные по категориям
        for (Transaction transaction : transactions) {
            String category = transaction.getCategory();
            float amount = (float) transaction.getAmount();
            if (categoryAmountMap.containsKey(category)) {
                categoryAmountMap.put(category, categoryAmountMap.get(category) + amount);
            } else {
                categoryAmountMap.put(category, amount);
            }
        }

        boolean hasNonZeroValues = false;
        for (float value : categoryAmountMap.values()) {
            if (value > 0) {
                hasNonZeroValues = true;
                break;
            }
        }

        if (!hasNonZeroValues) {
            pieChart.clear();
            pieChart.setVisibility(View.GONE); // Скрываем диаграмму и освобождаем место
            pieChart.invalidate();
            return;
        }

        // Показываем диаграмму, если есть данные
        pieChart.setVisibility(View.VISIBLE);

        // Создаем записи для диаграммы
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryAmountMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        // Настройка данных для диаграммы
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{0xFF77DD77, 0xFF89CFF0, 0xFFFF6961}); // Цвета для категорий
        dataSet.setValueTextSize(12f); // Размер текста значений
        dataSet.setValueTextColor(Color.BLACK); // Цвет текста значений

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        pieChart.animateY(1000, Easing.EaseInOutQuad); // Плавная анимация с ускорением и замедлением

        // Обновление диаграммы
        pieChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}