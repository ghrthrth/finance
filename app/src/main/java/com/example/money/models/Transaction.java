package com.example.money.models;

import java.util.Date;

public class Transaction {
    private int id;
    private String category;
    private double amount;
    private Date date; // Новое поле для даты и времени

    public Transaction(int id, String category, double amount, Date date) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public Date getDate() {
        return date;
    }
}