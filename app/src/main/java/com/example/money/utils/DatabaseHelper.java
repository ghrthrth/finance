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
    private static final int DATABASE_VERSION = 5; // Увеличиваем версию базы данных

    // Таблица категорий
    private static final String TABLE_CATEGORIES = "categories";
    private static final String COLUMN_CATEGORY_ID = "id";
    private static final String COLUMN_CATEGORY_NAME = "name";
    private static final String COLUMN_CATEGORY_COLOR = "color"; // Новое поле для цвета

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
                + COLUMN_CATEGORY_NAME + " TEXT UNIQUE,"
                + COLUMN_CATEGORY_COLOR + " INTEGER)"; // Новое поле для цвета
        db.execSQL(CREATE_CATEGORIES_TABLE);

        // Создание таблицы транзакций
        String CREATE_TRANSACTIONS_TABLE = "CREATE TABLE " + TABLE_TRANSACTIONS + "("
                + COLUMN_TRANSACTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TRANSACTION_CATEGORY + " TEXT,"
                + COLUMN_TRANSACTION_AMOUNT + " REAL,"
                + COLUMN_TRANSACTION_DATE + " INTEGER,"
                + COLUMN_TRANSACTION_TYPE + " INTEGER)"; // Поле для типа транзакции
        db.execSQL(CREATE_TRANSACTIONS_TABLE);

        Log.d("DatabaseHelper", "Таблица categories создана");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DatabaseHelper", "Обновление базы данных с версии " + oldVersion + " до " + newVersion);

        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + COLUMN_TRANSACTION_TYPE + " INTEGER DEFAULT 0");
            Log.d("DatabaseHelper", "Добавлен столбец " + COLUMN_TRANSACTION_TYPE);
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_CATEGORIES + " ADD COLUMN " + COLUMN_CATEGORY_COLOR + " INTEGER DEFAULT " + Color.GRAY);
            Log.d("DatabaseHelper", "Добавлен столбец " + COLUMN_CATEGORY_COLOR);
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
// Метод для получения всех категорий с их цветами
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES, null);

        if (cursor.moveToFirst()) {
            do {
                // Проверяем, существует ли столбец
                int idIndex = cursor.getColumnIndex(COLUMN_CATEGORY_ID);
                int nameIndex = cursor.getColumnIndex(COLUMN_CATEGORY_NAME);
                int colorIndex = cursor.getColumnIndex(COLUMN_CATEGORY_COLOR);

                if (idIndex != -1 && nameIndex != -1 && colorIndex != -1) {
                    int id = cursor.getInt(idIndex);
                    String name = cursor.getString(nameIndex);
                    int color = cursor.getInt(colorIndex);
                    categories.add(new Category(id, name, color)); // Используем правильный конструктор
                } else {
                    Log.e("DatabaseHelper", "Один из столбцов не найден в таблице categories");
                }
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
    public List<Transaction> getTransactionsByType(int type) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + COLUMN_TRANSACTION_TYPE + " = ?",
                new String[]{String.valueOf(type)}
        );

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0); // COLUMN_TRANSACTION_ID
                String category = cursor.getString(1); // COLUMN_TRANSACTION_CATEGORY
                double amount = cursor.getDouble(2); // COLUMN_TRANSACTION_AMOUNT
                long dateMillis = cursor.getLong(3); // COLUMN_TRANSACTION_DATE
                Date date = new Date(dateMillis);
                transactions.add(new Transaction(id, category, amount, date, type));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactions;
    }

    // Метод для получения цвета категории по её названию
    public int getCategoryColor(String categoryName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_CATEGORY_COLOR + " FROM " + TABLE_CATEGORIES + " WHERE " + COLUMN_CATEGORY_NAME + " = ?",
                new String[]{categoryName}
        );

        int color = Color.GRAY; // Цвет по умолчанию, если категория не найдена
        if (cursor.moveToFirst()) {
            color = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return color;
    }
    public void clearTransactions() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, null, null); // Удаляем все записи из таблицы
        db.close();
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

    // Метод для проверки, совпадает ли дата транзакции с выбранной датой
    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }
}