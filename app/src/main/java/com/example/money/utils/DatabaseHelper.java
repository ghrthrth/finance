package com.example.money.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.money.models.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "money.db";
    private static final int DATABASE_VERSION = 4; // Увеличиваем версию базы данных

    // Таблица категорий
    private static final String TABLE_CATEGORIES = "categories";
    private static final String COLUMN_CATEGORY_ID = "id";
    private static final String COLUMN_CATEGORY_NAME = "name";

    // Таблица транзакций
    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String COLUMN_TRANSACTION_ID = "id";
    private static final String COLUMN_TRANSACTION_CATEGORY = "category";
    private static final String COLUMN_TRANSACTION_AMOUNT = "amount";
    private static final String COLUMN_TRANSACTION_DATE = "date"; // Поле для даты и времени
    private static final String COLUMN_TRANSACTION_TYPE = "type"; // Поле для типа транзакции (доход/расход)

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Создание таблицы категорий
        String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + TABLE_CATEGORIES + "("
                + COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_CATEGORY_NAME + " TEXT UNIQUE)";
        db.execSQL(CREATE_CATEGORIES_TABLE);

        // Создание таблицы транзакций
        String CREATE_TRANSACTIONS_TABLE = "CREATE TABLE " + TABLE_TRANSACTIONS + "("
                + COLUMN_TRANSACTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TRANSACTION_CATEGORY + " TEXT,"
                + COLUMN_TRANSACTION_AMOUNT + " REAL,"
                + COLUMN_TRANSACTION_DATE + " INTEGER,"
                + COLUMN_TRANSACTION_TYPE + " INTEGER)"; // Поле для типа транзакции
        db.execSQL(CREATE_TRANSACTIONS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + COLUMN_TRANSACTION_TYPE + " INTEGER");
        }
    }

    // Метод для добавления категории
    public void addCategory(String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, category);
        db.insert(TABLE_CATEGORIES, null, values);
        db.close();
    }

    // Метод для получения всех категорий
    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES, null);

        if (cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(1)); // Индекс 1 соответствует COLUMN_CATEGORY_NAME
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return categories;
    }

    // Метод для добавления транзакции с датой, временем и типом
    public void addTransaction(String category, double amount, Date date, int type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TRANSACTION_CATEGORY, category);
        values.put(COLUMN_TRANSACTION_AMOUNT, amount);
        values.put(COLUMN_TRANSACTION_DATE, date.getTime());
        values.put(COLUMN_TRANSACTION_TYPE, type);
        db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
    }

    // Метод для получения всех транзакций
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRANSACTIONS, null);

        // Log column names for debugging
        String[] columnNames = cursor.getColumnNames();
        for (String name : columnNames) {
            Log.d("DatabaseHelper", "Column: " + name);
        }

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0); // COLUMN_TRANSACTION_ID
                String category = cursor.getString(1); // COLUMN_TRANSACTION_CATEGORY
                double amount = cursor.getDouble(2); // COLUMN_TRANSACTION_AMOUNT
                long dateMillis = cursor.getLong(3); // COLUMN_TRANSACTION_DATE
                int type = cursor.getInt(4); // COLUMN_TRANSACTION_TYPE
                Date date = new Date(dateMillis);
                transactions.add(new Transaction(id, category, amount, date, type));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactions;
    }

    // Метод для удаления категории
    public void deleteCategory(String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CATEGORIES, COLUMN_CATEGORY_NAME + " = ?", new String[]{category});
        db.close();
    }

    // Метод для удаления транзакции
    public void deleteTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, COLUMN_TRANSACTION_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Метод для получения транзакций по типу (доход/расход)
    public List<Transaction> getTransactionsByType(String type) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + COLUMN_TRANSACTION_TYPE + " = ?", new String[]{type});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0); // Индекс 0 соответствует COLUMN_TRANSACTION_ID
                String category = cursor.getString(1); // Индекс 1 соответствует COLUMN_TRANSACTION_CATEGORY
                double amount = cursor.getDouble(2); // Индекс 2 соответствует COLUMN_TRANSACTION_AMOUNT
                long dateMillis = cursor.getLong(3); // Индекс 3 соответствует COLUMN_TRANSACTION_DATE
                Date date = new Date(dateMillis); // Преобразуем timestamp в Date
                int transactionType = cursor.getInt(4); // Переименовали переменную
                transactions.add(new Transaction(id, category, amount, date, transactionType));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactions;
    }
}