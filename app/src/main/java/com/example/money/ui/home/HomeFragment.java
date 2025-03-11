package com.example.money.ui.home;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.money.R;
import com.example.money.databinding.FragmentHomeBinding;
import com.example.money.http.HttpRequestCallback;
import com.example.money.http.HttpRequestTask;
import com.example.money.models.Category;
import com.example.money.models.Transaction;
import com.example.money.ui.category.CategorySelectionFragment;
import com.example.money.utils.DatabaseHelper;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

    private int currentTransactionType = 1; // 0 - доходы, 1 - расходы (по умолчанию расходы)

    private String selectedCategory; // Переменная для хранения выбранной категории
    private String amountStr = ""; // Переменная для хранения введенной суммы
    private Date selectedDate; // Переменная для хранения выбранной даты и времени

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

        // Обработчик нажатия на заголовок "Учет расходов"
        binding.tvTitle.setOnClickListener(v -> showFunctionalitySelectionDialog());

        // Очищаем список транзакций перед загрузкой новых данных
        transactions.clear();

        // Загрузка транзакций из базы данных (только расходы, так как currentTransactionType = 1 по умолчанию)
        transactions.addAll(databaseHelper.getTransactionsByType(currentTransactionType)); // Загружаем только расходы
        adapter.notifyDataSetChanged();

        updateChart(); // Обновление диаграммы

        pieChart.animateY(1000, Easing.EaseInOutQuad); // Плавная анимация с ускорением и замедлением

        return root;
    }
    private void showFunctionalitySelectionDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_functionality_selection, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        Button btnIncome = bottomSheetView.findViewById(R.id.btnIncome);
        Button btnExpense = bottomSheetView.findViewById(R.id.btnExpense);

        btnIncome.setOnClickListener(v -> {
            setTransactionType(0); // Устанавливаем тип "Доходы"
            bottomSheetDialog.dismiss();
        });

        btnExpense.setOnClickListener(v -> {
            setTransactionType(1); // Устанавливаем тип "Расходы"
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void loadIncomeFunctionality() {
        // Логика для загрузки функционала доходов
        transactions.clear();
        transactions.addAll(databaseHelper.getTransactionsByType(0)); // 0 - тип для доходов
        adapter.notifyDataSetChanged();
        updateChart();
        Toast.makeText(requireContext(), "Загружены доходы", Toast.LENGTH_SHORT).show();
    }

    private void loadExpenseFunctionality() {
        // Логика для загрузки функционала расходов
        transactions.clear();
        transactions.addAll(databaseHelper.getTransactionsByType(1)); // 1 - тип для расходов
        adapter.notifyDataSetChanged();
        updateChart();
        Toast.makeText(requireContext(), "Загружены расходы", Toast.LENGTH_SHORT).show();
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

                    // Обновляем список транзакций (только для текущего типа)
                    transactions.clear();
                    transactions.addAll(databaseHelper.getTransactionsByType(currentTransactionType)); // Загружаем только текущий тип
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
        Button btnChooseDateTime = bottomSheetView.findViewById(R.id.btnChooseDateTime); // Одна кнопка для выбора даты и времени
        RadioGroup radioGroupTransactionType = bottomSheetView.findViewById(R.id.radioGroupTransactionType);
        RadioButton radioIncome = bottomSheetView.findViewById(R.id.radioIncome);
        RadioButton radioExpense = bottomSheetView.findViewById(R.id.radioExpense);

        // Восстанавливаем ранее введенную сумму (если есть)
        if (!amountStr.isEmpty()) {
            etAmount.setText(amountStr);
        }

        // Восстанавливаем выбранную дату и время (если есть)
        if (selectedDate != null) {
            btnChooseDateTime.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(selectedDate));
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

        btnChooseDateTime.setOnClickListener(v -> {
            // Сначала показываем DatePickerDialog для выбора даты
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, year1, month1, dayOfMonth) -> {
                // Сохраняем выбранную дату
                calendar.set(year1, month1, dayOfMonth);

                // После выбора даты показываем TimePickerDialog для выбора времени
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (view1, hourOfDay, minute1) -> {
                    // Сохраняем выбранное время
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute1);
                    selectedDate = calendar.getTime();

                    // Обновляем текст на кнопке
                    btnChooseDateTime.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(selectedDate));
                }, hour, minute, true);
                timePickerDialog.show();
            }, year, month, day);
            datePickerDialog.show();
        });

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (!amountStr.isEmpty() && selectedCategory != null && selectedDate != null) {
                double amount = Double.parseDouble(amountStr); // Преобразуем сумму в double

                // Определяем тип транзакции
                int type = radioIncome.isChecked() ? 0 : 1;

                // Создаем параметры для отправки на сервер
                Map<String, String> params = new HashMap<>();
                params.put("category", selectedCategory);
                params.put("amount", String.valueOf(amount));
                params.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(selectedDate)); // Форматируем дату
                params.put("type", String.valueOf(type)); // Тип транзакции (0 - доход, 1 - расход)

                // Создаем и выполняем задачу HTTP-запроса
                HttpRequestTask task = new HttpRequestTask(
                        requireContext(),
                        "https://claimbes.store/spend_smart/api/add_transaction.php", // Укажите URL для добавления транзакции
                        params,
                        "POST",
                        new HttpRequestCallback() {
                            @Override
                            public void onSuccess(String response) {
                                // Обработка успешного ответа от сервера
                                Toast.makeText(requireContext(), "Транзакция добавлена: " + response, Toast.LENGTH_SHORT).show();

                                // Добавляем транзакцию в локальную базу данных
                                databaseHelper.addTransaction(selectedCategory, amount, selectedDate, type);

                                // Обновляем список транзакций (только для текущего типа)
                                transactions.clear();
                                transactions.addAll(databaseHelper.getTransactionsByType(currentTransactionType));
                                adapter.notifyDataSetChanged();

                                // Обновляем диаграмму
                                updateChart();

                                // Закрываем BottomSheetDialog
                                bottomSheetDialog.dismiss();
                                Log.d("testfefr", "ffergf" + response);
                            }

                            @Override
                            public void onFailure(String error) {
                                // Обработка ошибки
                                Toast.makeText(requireContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                                Log.d("teewest", "ffergf" + error);
                            }
                        });

                task.execute();
            } else {
                Toast.makeText(requireContext(), "Введите сумму, выберите категорию и дату", Toast.LENGTH_SHORT).show();
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
        List<Transaction> transactions = databaseHelper.getTransactionsByType(currentTransactionType);
        Map<String, Float> dataMap = new HashMap<>();
        Map<String, Integer> colorMap = new HashMap<>(); // Для хранения цветов категорий

        // Получаем все категории с их цветами
        List<Category> categories = databaseHelper.getAllCategories();
        for (Category category : categories) {
            colorMap.put(category.getName(), category.getColor()); // Сохраняем цвета
        }

        // Суммируем суммы по категориям
        for (Transaction transaction : transactions) {
            String category = transaction.getCategory();
            float amount = (float) transaction.getAmount();
            dataMap.put(category, dataMap.getOrDefault(category, 0f) + amount);
        }

        // Подготавливаем данные для диаграммы
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>(); // Список цветов для каждой категории

        for (Map.Entry<String, Float> entry : dataMap.entrySet()) {
            String category = entry.getKey();
            Integer color = colorMap.get(category);
            if (color != null) {
                entries.add(new PieEntry(entry.getValue(), category));
                colors.add(color); // Добавляем цвет категории
            }
        }

        // Обновляем график
        updatePieChart(entries, colors, currentTransactionType == 0 ? "Доходы" : "Расходы");
    }

    public void setTransactionType(int type) {
        this.currentTransactionType = type; // Устанавливаем тип (0 - доходы, 1 - расходы)

        if (type == 0) {
            loadIncomeFunctionality(); // Загружаем доходы
            binding.tvTitle.setText("Учет доходов"); // Обновляем заголовок
        } else {
            loadExpenseFunctionality(); // Загружаем расходы
            binding.tvTitle.setText("Учет расходов"); // Обновляем заголовок
        }

        updateChart(); // Обновляем график
    }
    private void updatePieChart(List<PieEntry> entries, List<Integer> colors, String label) {
        if (entries == null || entries.isEmpty() || colors == null || colors.isEmpty()) {
            pieChart.clear();
            pieChart.setVisibility(View.GONE);
            pieChart.invalidate();
            return;
        }

        pieChart.setVisibility(View.VISIBLE);

        PieDataSet dataSet = new PieDataSet(entries, label);
        dataSet.setColors(colors); // Устанавливаем цвета для каждой категории
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        pieChart.animateY(1000, Easing.EaseInOutQuad);
        pieChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}