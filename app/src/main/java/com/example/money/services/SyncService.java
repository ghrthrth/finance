package com.example.money.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.money.http.HttpRequestCallback;
import com.example.money.http.HttpRequestTask;
import com.example.money.models.Transaction;
import com.example.money.utils.DatabaseHelper;
import com.example.money.utils.NetworkUtils;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncService extends IntentService {

    public SyncService() {
        super("SyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Проверяем наличие интернета
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.d("SyncService", "Интернет отсутствует, синхронизация невозможна");
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        List<Transaction> unsyncedTransactions = dbHelper.getUnsyncedTransactions();

        if (unsyncedTransactions.isEmpty()) {
            Log.d("SyncService", "Нет несинхронизированных транзакций");
            return;
        }

        for (Transaction transaction : unsyncedTransactions) {
            if (sendTransactionToServer(transaction)) {
                dbHelper.markTransactionAsSynced(transaction.getId());
                Log.d("SyncService", "Транзакция синхронизирована: " + transaction.getId());
            } else {
                Log.e("SyncService", "Ошибка синхронизации транзакции: " + transaction.getId());
            }
        }
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

    private int getCurrentUserId() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("user_prefs", MODE_PRIVATE);
        if (!sharedPreferences.contains("user_id")) {
            Log.e("getCurrentUserId", "Ошибка: пользователь не авторизован");
            return -1;
        }
        return sharedPreferences.getInt("user_id", -1);
    }
}