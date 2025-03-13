package com.example.money.ui.create_news;

import static android.content.Context.MODE_PRIVATE;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.money.R;
import com.example.money.databinding.FragmentCreateNewsBinding;
import com.example.money.http.HttpRequestCallback;
import com.example.money.http.HttpRequestTask;
import com.example.money.models.Transaction;
import com.example.money.ui.home.TransactionAdapter;
import com.example.money.utils.DatabaseHelper;
import com.example.money.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateNewsFragment extends Fragment {

    private FragmentCreateNewsBinding binding;
    private DatabaseHelper databaseHelper;
    private TextView tvIncome, tvExpense, tvTotalAmount;
    private RecyclerView recyclerViewTransactions;
    private TransactionAdapter transactionAdapter;
    private Date selectedDate = null; // Выбранная дата для фильтрации
    private int filterType = -1; // -1 - все, 0 - доходы, 1 - расходы

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCreateNewsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Инициализация DatabaseHelper
        databaseHelper = new DatabaseHelper(requireContext());

        // Инициализация TextView
        tvIncome = binding.tvIncome;
        tvExpense = binding.tvExpense;
        tvTotalAmount = binding.tvTotalAmount;

        // Инициализация RecyclerView
        recyclerViewTransactions = binding.recyclerViewTransactions;
        recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(getContext()));

        // Загрузка и отображение данных
        loadTransactions();

        // Обработчик для кнопки выбора даты
        binding.btnFilterDate.setOnClickListener(v -> showDatePickerDialog());

        // Обработчик для RadioGroup (фильтр по типу)
        binding.radioGroupFilterType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioAll) {
                filterType = -1; // Все транзакции
            } else if (checkedId == R.id.radioIncome) {
                filterType = 0; // Доходы
            } else if (checkedId == R.id.radioExpense) {
                filterType = 1; // Расходы
            }
            applyFilter(); // Применяем фильтр
        });

        return root;
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, year1, month1, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year1, month1, dayOfMonth);
            selectedDate = selectedCalendar.getTime();
            applyFilter(); // Применяем фильтр после выбора даты
        }, year, month, day);
        datePickerDialog.show();
    }

    private void applyFilter() {
        // Получаем все транзакции из базы данных
        List<Transaction> allTransactions = databaseHelper.getAllTransactions();

        // Фильтруем транзакции
        List<Transaction> filteredTransactions = filterTransactions(allTransactions, selectedDate, filterType);

        // Обновляем адаптер
        transactionAdapter = new TransactionAdapter(filteredTransactions, transaction -> {
            // Обработка нажатия на элемент списка
            Toast.makeText(requireContext(), "Выбрана транзакция: " + transaction.getCategory(), Toast.LENGTH_SHORT).show();
        });
        recyclerViewTransactions.setAdapter(transactionAdapter);

        // Пересчитываем суммы доходов и расходов
        updateTotals(filteredTransactions);
    }

    private void loadTransactions() {
        // Проверяем, есть ли интернет
        if (NetworkUtils.isOnline(requireContext())) {
            // Если онлайн, загружаем данные с сервера
            loadTransactionsFromServer();
        } else {
            // Если оффлайн, загружаем данные из локальной базы данных
            loadTransactionsFromLocalDatabase();
        }
    }

    private void loadTransactionsFromServer() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("user_prefs", MODE_PRIVATE);
        // Проверяем, существует ли ключ "user_id"
        if (!sharedPreferences.contains("user_id")) {
            Toast.makeText(requireContext(), "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        // Получаем user_id из SharedPreferences
        int userId = sharedPreferences.getInt("user_id", -1); // Значение по умолчанию не будет использоваться, так как мы уже пров

        // Создаем параметры для отправки на сервер
        Map<String, String> params = new HashMap<>();
        params.put("user_id", String.valueOf(userId));

        // Создаем и выполняем задачу HTTP-запроса
        HttpRequestTask task = new HttpRequestTask(
                requireContext(),
                "https://claimbes.store/spend_smart/api/get_transactions.php", // Укажите URL для получения транзакций
                params,
                "GET",
                new HttpRequestCallback() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            // Парсим JSON-ответ от сервера
                            JSONArray jsonArray = new JSONArray(response);

                            // Очищаем старые данные в локальной базе данных

                            // Очищаем текущий список транзакций
                            List<Transaction> transactions = new ArrayList<>();

                            // Добавляем транзакции из JSON-ответа
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String category = jsonObject.getString("category");
                                double amount = jsonObject.getDouble("amount");
                                String dateStr = jsonObject.getString("date");
                                int type = jsonObject.getInt("type");
                                int id = jsonObject.getInt("id");

                                databaseHelper.clearTransactions();

                                // Преобразуем строку даты в объект Date
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                Date date = format.parse(dateStr);

                                // Создаем объект Transaction и добавляем его в список
                                Transaction transaction = new Transaction(id, category, amount, date, type);
                                transactions.add(transaction);

                                // Сохраняем транзакцию в локальную базу данных
                                databaseHelper.addTransaction(category, amount, date, type);
                            }

                            // Обновляем адаптер
                            transactionAdapter = new TransactionAdapter(transactions, transaction -> {
                                Toast.makeText(requireContext(), "Выбрана транзакция: " + transaction.getCategory(), Toast.LENGTH_SHORT).show();
                            });
                            recyclerViewTransactions.setAdapter(transactionAdapter);

                            // Пересчитываем суммы доходов и расходов
                            updateTotals(transactions);

                            Log.d("LoadTransactions", "Транзакции успешно загружены с сервера и обновлены в локальной БД");
                        } catch (JSONException | ParseException e) {
                            e.printStackTrace();
                            Toast.makeText(requireContext(), "Ошибка при загрузке транзакций", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(requireContext(), "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                        Log.e("LoadTransactions", "Ошибка: " + error);
                    }
                });

        task.execute();
    }

    private void loadTransactionsFromLocalDatabase() {
        // Получаем все транзакции из локальной базы данных
        List<Transaction> allTransactions = databaseHelper.getAllTransactions();

        // Обновляем адаптер
        transactionAdapter = new TransactionAdapter(allTransactions, transaction -> {
            Toast.makeText(requireContext(), "Выбрана транзакция: " + transaction.getCategory(), Toast.LENGTH_SHORT).show();
        });
        recyclerViewTransactions.setAdapter(transactionAdapter);

        // Пересчитываем суммы доходов и расходов
        updateTotals(allTransactions);
    }

    private List<Transaction> filterTransactions(List<Transaction> transactions, Date selectedDate, int filterType) {
        List<Transaction> filteredTransactions = new ArrayList<>();

        for (Transaction transaction : transactions) {
            // Фильтр по типу (0 - доходы, 1 - расходы, -1 - все)
            boolean matchesType = (filterType == -1) || (transaction.getType() == filterType);

            // Фильтр по дате (если selectedDate не null)
            boolean matchesDate = (selectedDate == null) || isSameDay(transaction.getDate(), selectedDate);

            if (matchesType && matchesDate) {
                filteredTransactions.add(transaction);
            }
        }

        return filteredTransactions;
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    private void updateTotals(List<Transaction> transactions) {
        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction transaction : transactions) {
            if (transaction.getType() == 0) { // Доходы
                totalIncome += transaction.getAmount();
            } else if (transaction.getType() == 1) { // Расходы
                totalExpense += transaction.getAmount();
            }
        }

        // Общая сумма (доходы - расходы)
        double totalAmount = totalIncome - totalExpense;

        // Обновляем TextView
        tvIncome.setText("Доходы: " + totalIncome);
        tvExpense.setText("Расходы: " + totalExpense);
        tvTotalAmount.setText("Общая сумма: " + totalAmount);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}