package com.example.money.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.money.http.HttpRequestCallback;
import com.example.money.http.HttpRequestTask;
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

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.d("SyncService", "Интернет отсутствует, синхронизация невозможна");
            return;
        }

        fetchTransactionsFromServer();
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

        // Проверяем, есть ли на сервере транзакции, которых нет локально
        for (Transaction serverTransaction : serverTransactions) {
            if (!localTransactionIds.contains(serverTransaction.getId())) {
                // Если транзакции нет локально, удаляем её на сервере
                deleteTransactionOnServer(serverTransaction.getId());
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
                sendTransactionToServer(localTransaction);
                dbHelper.markTransactionAsSynced(localTransaction.getId()); // Помечаем как синхронизированную
            }
        }
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

                            // Обработка JSON-ответа
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
}