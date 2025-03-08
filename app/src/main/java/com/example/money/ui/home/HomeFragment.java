package com.example.money.ui.home;

import android.app.AlertDialog;
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
import com.example.money.models.Transaction;
import com.example.money.ui.category.CategorySelectionFragment;
import com.example.money.utils.DatabaseHelper;
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

public class HomeFragment extends Fragment implements CategorySelectionFragment.OnCategorySelectedListener, TransactionAdapter.OnItemClickListener {

    private FragmentHomeBinding binding;
    private PieChart pieChart;
    private RecyclerView recyclerView;
    private Button btnAddTransaction;
    private List<Transaction> transactions = new ArrayList<>();
    private TransactionAdapter adapter;
    private DatabaseHelper databaseHelper;

    private String selectedCategory; // Переменная для хранения выбранной категории
    private String amountStr = ""; // Переменная для хранения введенной суммы

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d("HomeFragment", "onCreateView called");

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Инициализация DatabaseHelper
        databaseHelper = new DatabaseHelper(requireContext());

        // Инициализация элементов интерфейса
        pieChart = binding.pieChart;
        btnAddTransaction = binding.btnAddTransaction;
        recyclerView = binding.recyclerView;

        // Настройка RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(transactions, this); // Передаем this как слушатель
        recyclerView.setAdapter(adapter);

        pieChart.getDescription().setEnabled(false); // Отключение описания (label) диаграммы

        // Обработчик нажатия на кнопку
        btnAddTransaction.setOnClickListener(v -> addTransaction());

        transactions.clear();
        transactions.addAll(databaseHelper.getAllTransactions());
        adapter.notifyDataSetChanged(); // Обновляем адаптер

        updateChart(); // Обновление диаграммы

        pieChart.animateY(1000, Easing.EaseInOutQuad); // Плавная анимация с ускорением и замедлением

        return root;
    }
    @Override
    public void onItemClick(Transaction transaction) {
        // Показываем диалог подтверждения удаления
        showDeleteConfirmationDialog(transaction);
    }
    private void showDeleteConfirmationDialog(Transaction transaction) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удаление транзакции")
                .setMessage("Вы уверены, что хотите удалить эту транзакцию?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    // Удаляем транзакцию из базы данных
                    databaseHelper.deleteTransaction(transaction.getId());

                    // Обновляем список транзакций
                    transactions.clear();
                    transactions.addAll(databaseHelper.getAllTransactions());
                    adapter.notifyDataSetChanged();

                    // Обновляем диаграмму
                    updateChart();

                    Toast.makeText(requireContext(), "Транзакция удалена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
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
                double amount = Double.parseDouble(amountStr);

                // Добавляем транзакцию в базу данных
                databaseHelper.addTransaction(selectedCategory, amount);

                // Обновляем список транзакций
                transactions.clear();
                transactions.addAll(databaseHelper.getAllTransactions());
                adapter.notifyDataSetChanged();

                // Обновляем диаграмму
                updateChart();

                // Очищаем состояние BottomSheetDialog
                etAmount.setText("");
                selectedCategory = null;
                amountStr = "";

                // Закрываем BottomSheetDialog
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
        // Получаем все транзакции из базы данных
        List<Transaction> transactions = databaseHelper.getAllTransactions();

        // Создаем карту для группировки транзакций по категориям
        Map<String, Float> categoryAmountMap = new HashMap<>();

        // Собираем данные по категориям
        for (Transaction transaction : transactions) {
            String category = transaction.getCategory();
            float amount = (float) transaction.getAmount();

            if (categoryAmountMap.containsKey(category)) {
                // Если категория уже есть в карте, добавляем сумму
                categoryAmountMap.put(category, categoryAmountMap.get(category) + amount);
            } else {
                // Если категории нет в карте, добавляем новую запись
                categoryAmountMap.put(category, amount);
            }
        }

        // Проверяем, есть ли данные для отображения
        boolean hasNonZeroValues = false;
        for (float value : categoryAmountMap.values()) {
            if (value > 0) {
                hasNonZeroValues = true;
                break;
            }
        }

        if (!hasNonZeroValues) {
            // Если данных нет, скрываем диаграмму
            pieChart.clear();
            pieChart.setVisibility(View.GONE);
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

        // Анимация диаграммы
        pieChart.animateY(1000, Easing.EaseInOutQuad);

        // Обновление диаграммы
        pieChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}