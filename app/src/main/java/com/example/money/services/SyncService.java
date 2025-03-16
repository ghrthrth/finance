package com.example.money.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.money.http.HttpRequestCallback;
import com.example.money.http.HttpRequestTask;
import com.example.money.models.Category;
import com.example.money.models.Transaction;
import com.example.money.utils.DatabaseHelper;
import com.example.money.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SyncService extends IntentService {

    public SyncService() {
        super("SyncService");
    }

    public static void startSync(Context context) {
        Intent intent = new Intent(context, SyncService.class);
        context.startService(intent);
    }

    private boolean isRestoring = false; // Флаг восстановления данных

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.d("SyncService", "Интернет отсутствует, синхронизация невозможна");
            return;
        }

        // Восстановление данных при первом запуске
        restoreDataFromServer();

        // Регулярная синхронизация
        fetchTransactionsFromServer();
        fetchCategoriesFromServer(); // Синхронизация категорий
    }

    private boolean sendTransactionToServer(Transaction transaction) {
        Map<String, String> params = new HashMap<>();
        params.put("category", transaction.getCategory());
        params.put("amount", String.valueOf(transaction.getAmount()));
        params.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(transaction.getDate()));
        params.put("type", String.valueOf(transaction.getType()));
        params.put("user_id", String.valueOf(getCurrentUserId()));

        try {
            HttpRequestTask task = new HttpRequestTask(
                    this,
                    "https://claimbes.store/spend_smart/api/add_transaction.php",
                    params,
                    "POST",
                    new HttpRequestCallback() {
                        @Override
                        public void onSuccess(String response) {
                            Log.d("SyncService", "Транзакция успешно отправлена на сервер: " + response);
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e("SyncService", "Ошибка при отправке транзакции: " + error);
                        }
                    });

            task.execute();
            return true;
        } catch (Exception e) {
            Log.e("SyncService", "Ошибка: " + e.getMessage());
            return false;
        }
    }

    private void deleteTransactionOnServer(long transactionId) {
        Map<String, String> params = new HashMap<>();
        params.put("transaction_id", String.valueOf(transactionId));
        params.put("user_id", String.valueOf(getCurrentUserId()));

        try {
            HttpRequestTask task = new HttpRequestTask(
                    this,
                    "https://claimbes.store/spend_smart/api/delete_transaction.php",
                    params,
                    "POST",
                    new HttpRequestCallback() {
                        @Override
                        public void onSuccess(String response) {
                            Log.d("SyncService", "Транзакция успешно удалена на сервере: " + response);
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e("SyncService", "Ошибка при удалении транзакции: " + error);
                        }
                    });

            task.execute();
        } catch (Exception e) {
            Log.e("SyncService", "Ошибка: " + e.getMessage());
        }
    }
    private void compareAndSyncTransactions(List<Transaction> serverTransactions, DatabaseHelper dbHelper) {
        // Получаем все транзакции из локальной базы данных
        List<Transaction> localTransactions = dbHelper.getAllTransactions();

        // Создаем набор ID локальных транзакций для быстрого поиска
        Set<Long> localTransactionIds = new HashSet<>();
        for (Transaction transaction : localTransactions) {
            localTransactionIds.add(transaction.getId());
        }

        // Если данные не восстанавливаются, проверяем, есть ли на сервере транзакции, которых нет локально
        if (!isRestoring) {
            for (Transaction serverTransaction : serverTransactions) {
                if (!localTransactionIds.contains(serverTransaction.getId())) {
                    // Если транзакции нет локально, удаляем её на сервере
                    deleteTransactionOnServer(serverTransaction.getId());
                }
            }
        }

        // Проверяем, есть ли в локальной базе данных транзакции, которых нет на сервере
        Set<Long> serverTransactionIds = new HashSet<>();
        for (Transaction transaction : serverTransactions) {
            serverTransactionIds.add(transaction.getId());
        }

        for (Transaction localTransaction : localTransactions) {
            if (!serverTransactionIds.contains(localTransaction.getId())) {
                // Если транзакции нет на сервере, отправляем её
                if (!isRestoring) { // Не отправляем транзакции при восстановлении
                    sendTransactionToServer(localTransaction);
                }
                dbHelper.markTransactionAsSynced(localTransaction.getId()); // Помечаем как синхронизированную
            }
        }

        // Сбрасываем флаг восстановления после завершения синхронизации
        isRestoring = false;
    }
    private void fetchTransactionsFromServer() {
        Map<String, String> params = new HashMap<>();
        params.put("user_id", String.valueOf(getCurrentUserId()));

        try {
            HttpRequestTask task = new HttpRequestTask(
                    this,
                    "https://claimbes.store/spend_smart/api/get_transactions.php",
                    params,
                    "GET",
                    new HttpRequestCallback() {
                        @Override
                        public void onSuccess(String response) {
                            Log.d("SyncService", "Транзакции успешно получены: " + response);

                            try {
                                JSONArray transactionsArray = new JSONArray(response);
                                DatabaseHelper dbHelper = new DatabaseHelper(SyncService.this);

                                // Формат даты, который возвращает сервер
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                                // Список транзакций с сервера
                                List<Transaction> serverTransactions = new ArrayList<>();

                                // Преобразуем JSON в список транзакций
                                for (int i = 0; i < transactionsArray.length(); i++) {
                                    JSONObject transactionJson = transactionsArray.getJSONObject(i);

                                    long transactionId = transactionJson.getLong("id");
                                    String category = transactionJson.getString("category");
                                    double amount = transactionJson.getDouble("amount");
                                    String dateString = transactionJson.getString("date"); // Получаем дату как строку
                                    int type = transactionJson.getInt("type");

                                    // Преобразуем строку даты в объект Date
                                    Date date = dateFormat.parse(dateString);

                                    // Создаем объект Transaction
                                    Transaction transaction = new Transaction((int) transactionId, category, amount, date, type);
                                    serverTransactions.add(transaction);

                                    // Если локальная база данных пуста, сохраняем транзакцию локально
                                    if (dbHelper.getAllTransactions().isEmpty()) {
                                        dbHelper.addTransaction(category, amount, date, type);
                                    }
                                }

                                // Сравниваем транзакции и синхронизируем
                                compareAndSyncTransactions(serverTransactions, dbHelper);

                                Log.d("SyncService", "Локальная база данных успешно обновлена");
                            } catch (JSONException e) {
                                Log.e("SyncService", "Ошибка при разборе JSON fetchTransactionsFromServer: " + e.getMessage());
                            } catch (ParseException e) {
                                Log.e("SyncService", "Ошибка при разборе даты: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e("SyncService", "Ошибка при получении транзакций: " + error);
                        }
                    });

            task.execute();
        } catch (Exception e) {
            Log.e("SyncService", "Ошибка: " + e.getMessage());
        }
    }

    private int getCurrentUserId() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("user_prefs", MODE_PRIVATE);
        if (!sharedPreferences.contains("user_id")) {
            Log.e("getCurrentUserId", "Ошибка: пользователь не авторизован");
            return -1;
        }
        return sharedPreferences.getInt("user_id", -1);
    }

    private void restoreDataFromServer() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);

        // Проверяем, есть ли локальные данные
        if (dbHelper.getAllTransactions().isEmpty() && dbHelper.getAllCategories().isEmpty()) {
            Log.d("SyncService", "Локальные данные отсутствуют, начинаем восстановление с сервера");
            isRestoring = true; // Устанавливаем флаг восстановления
            fetchTransactionsFromServer(); // Загружаем транзакции с сервера
            fetchCategoriesFromServer(); // Загружаем категории с сервера
        } else {
            Log.d("SyncService", "Локальные данные присутствуют, восстановление не требуется");
        }
    }

    private boolean sendCategoryToServer(Category category) {
        Map<String, String> params = new HashMap<>();
        params.put("name", category.getName());
        params.put("color", String.valueOf(category.getColor()));
        params.put("user_id", String.valueOf(getCurrentUserId()));

        try {
            HttpRequestTask task = new HttpRequestTask(
                    this,
                    "https://claimbes.store/spend_smart/api/add_category.php", // Укажите правильный URL
                    params,
                    "POST",
                    new HttpRequestCallback() {
                        @Override
                        public void onSuccess(String response) {
                            Log.d("SyncService", "Категория успешно отправлена на сервер: " + response);
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e("SyncService", "Ошибка при отправке категории: " + error);
                        }
                    });

            task.execute();
            return true;
        } catch (Exception e) {
            Log.e("SyncService", "Ошибка: " + e.getMessage());
            return false;
        }
    }

    private void deleteCategoryOnServer(int categoryId) {
        Map<String, String> params = new HashMap<>();
        params.put("category_id", String.valueOf(categoryId));
        params.put("user_id", String.valueOf(getCurrentUserId()));

        try {
            HttpRequestTask task = new HttpRequestTask(
                    this,
                    "https://claimbes.store/spend_smart/api/delete_category.php", // Укажите правильный URL
                    params,
                    "POST",
                    new HttpRequestCallback() {
                        @Override
                        public void onSuccess(String response) {
                            Log.d("SyncService", "Категория успешно удалена на сервере: " + response);
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e("SyncService", "Ошибка при удалении категории: " + error);
                        }
                    });

            task.execute();
        } catch (Exception e) {
            Log.e("SyncService", "Ошибка: " + e.getMessage());
        }
    }

    private void fetchCategoriesFromServer() {
        Map<String, String> params = new HashMap<>();
        params.put("user_id", String.valueOf(getCurrentUserId()));

        try {
            HttpRequestTask task = new HttpRequestTask(
                    this,
                    "https://claimbes.store/spend_smart/api/get_categories.php",
                    params,
                    "GET",
                    new HttpRequestCallback() {
                        @Override
                        public void onSuccess(String response) {
                            Log.d("SyncService", "Категории успешно получены: " + response);

                            try {
                                JSONArray categoriesArray = new JSONArray(response);
                                DatabaseHelper dbHelper = new DatabaseHelper(SyncService.this);

                                // Список категорий с сервера
                                List<Category> serverCategories = new ArrayList<>();

                                // Преобразуем JSON в список категорий
                                for (int i = 0; i < categoriesArray.length(); i++) {
                                    JSONObject categoryJson = categoriesArray.getJSONObject(i);

                                    int id = categoryJson.getInt("id");
                                    String name = categoryJson.getString("name");
                                    int color = categoryJson.getInt("color");

                                    // Создаем объект Category
                                    Category category = new Category(id, name, color);
                                    serverCategories.add(category);

                                    // Если локальная база данных пуста, сохраняем категорию локально
                                    if (dbHelper.getAllCategories().isEmpty()) {
                                        dbHelper.addCategory(name, color); // Используем метод с указанием цвета
                                    }
                                }

                                // Сравниваем категории и синхронизируем
                                compareAndSyncCategories(serverCategories, dbHelper);

                                Log.d("SyncService", "Локальная база данных категорий успешно обновлена");
                            } catch (JSONException e) {
                                Log.e("SyncService", "Ошибка при разборе JSON fetchCategoriesFromServer: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e("SyncService", "Ошибка при получении категорий: " + error);
                        }
                    });

            task.execute();
        } catch (Exception e) {
            Log.e("SyncService", "Ошибка: " + e.getMessage());
        }
    }

    private void compareAndSyncCategories(List<Category> serverCategories, DatabaseHelper dbHelper) {
        // Получаем все категории из локальной базы данных
        List<Category> localCategories = dbHelper.getAllCategories();

        // Создаем набор ID локальных категорий для быстрого поиска
        Set<Integer> localCategoryIds = new HashSet<>();
        for (Category category : localCategories) {
            localCategoryIds.add(category.getId());
        }

        // Если данные не восстанавливаются, проверяем, есть ли на сервере категории, которых нет локально
        if (!isRestoring) {
            for (Category serverCategory : serverCategories) {
                if (!localCategoryIds.contains(serverCategory.getId())) {
                    // Если категории нет локально, удаляем её на сервере
                    deleteCategoryOnServer(serverCategory.getId());
                }
            }
        }

        // Проверяем, есть ли в локальной базе данных категории, которых нет на сервере
        Set<Integer> serverCategoryIds = new HashSet<>();
        for (Category category : serverCategories) {
            serverCategoryIds.add(category.getId());
        }

        for (Category localCategory : localCategories) {
            if (!serverCategoryIds.contains(localCategory.getId())) {
                // Если категории нет на сервере, отправляем её
                if (!isRestoring) { // Не отправляем категории при восстановлении
                    sendCategoryToServer(localCategory);
                }
            }
        }

        // Сбрасываем флаг восстановления после завершения синхронизации
        isRestoring = false;
    }
}