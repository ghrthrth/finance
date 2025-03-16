package com.example.money.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.util.Log;

import com.example.money.models.Category;
import com.example.money.models.Transaction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "money.db";
    private static final int DATABASE_VERSION = 7; // Увеличиваем версию базы данных

    // Таблица категорий
    private static final String TABLE_CATEGORIES = "categories";
    private static final String COLUMN_CATEGORY_ID = "id";
    private static final String COLUMN_CATEGORY_NAME = "name";
    private static final String COLUMN_CATEGORY_COLOR = "color";

    // Таблица транзакций
    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String COLUMN_TRANSACTION_ID = "id";
    private static final String COLUMN_TRANSACTION_CATEGORY = "category";
    private static final String COLUMN_TRANSACTION_AMOUNT = "amount";
    private static final String COLUMN_TRANSACTION_DATE = "date";
    private static final String COLUMN_TRANSACTION_TYPE = "type";
    private static final String COLUMN_TRANSACTION_SYNCED = "synced";
    private static final String COLUMN_TRANSACTION_DELETED = "is_deleted";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Создание таблицы категорий
        String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + TABLE_CATEGORIES + "("
                + COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_CATEGORY_NAME + " TEXT,"
                + COLUMN_CATEGORY_COLOR + " INTEGER)";
        db.execSQL(CREATE_CATEGORIES_TABLE);

        // Создание таблицы транзакций
        String CREATE_TRANSACTIONS_TABLE = "CREATE TABLE " + TABLE_TRANSACTIONS + "("
                + COLUMN_TRANSACTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TRANSACTION_CATEGORY + " TEXT,"
                + COLUMN_TRANSACTION_AMOUNT + " REAL,"
                + COLUMN_TRANSACTION_DATE + " INTEGER,"
                + COLUMN_TRANSACTION_TYPE + " INTEGER,"
                + COLUMN_TRANSACTION_SYNCED + " INTEGER DEFAULT 0,"
                + COLUMN_TRANSACTION_DELETED + " INTEGER DEFAULT 0)";
        db.execSQL(CREATE_TRANSACTIONS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + COLUMN_TRANSACTION_DELETED + " INTEGER DEFAULT 0");
        }
    }

    // Метод для добавления категории с генерацией случайного цвета
    public void addCategory(String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, category);
        values.put(COLUMN_CATEGORY_COLOR, generateRandomColor()); // Генерация случайного цвета
        db.insert(TABLE_CATEGORIES, null, values);
        db.close();
    }

    // Метод для генерации случайного цвета
    private int generateRandomColor() {
        Random random = new Random();
        return Color.rgb(
                random.nextInt(256), // Красный
                random.nextInt(256), // Зеленый
                random.nextInt(256)  // Синий
        );
    }

    // Метод для получения всех категорий с их цветами
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                int color = cursor.getInt(2);
                categories.add(new Category(id, name, color));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return categories;
    }

    // Метод для добавления транзакции
    public long addTransaction(String category, double amount, Date date, int type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TRANSACTION_CATEGORY, category);
        values.put(COLUMN_TRANSACTION_AMOUNT, amount);
        values.put(COLUMN_TRANSACTION_DATE, date.getTime());
        values.put(COLUMN_TRANSACTION_TYPE, type);
        values.put(COLUMN_TRANSACTION_SYNCED, 0); // По умолчанию не синхронизировано
        long id = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
        return id;
    }


    // Метод для пометки транзакции как синхронизированной
    public void markTransactionAsSynced(long transactionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TRANSACTION_SYNCED, 1);
        db.update(TABLE_TRANSACTIONS, values, COLUMN_TRANSACTION_ID + " = ?", new String[]{String.valueOf(transactionId)});
        db.close();
    }

    // Метод для удаления транзакции
    public void deleteTransaction(long transactionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, COLUMN_TRANSACTION_ID + " = ?", new String[]{String.valueOf(transactionId)});
        db.close();
    }

    // Метод для получения всех транзакций
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRANSACTIONS, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String category = cursor.getString(1);
                double amount = cursor.getDouble(2);
                long dateMillis = cursor.getLong(3);
                int type = cursor.getInt(4);
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

    // Метод для получения транзакций по типу (доход/расход)
    public List<Transaction> getTransactionsByType(int type) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + COLUMN_TRANSACTION_TYPE + " = ?",
                new String[]{String.valueOf(type)}
        );

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String category = cursor.getString(1);
                double amount = cursor.getDouble(2);
                long dateMillis = cursor.getLong(3);
                Date date = new Date(dateMillis);
                transactions.add(new Transaction(id, category, amount, date, type));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactions;
    }
}