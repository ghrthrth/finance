package com.example.money.models;

public class Transaction {
    private int id;
    private String category;
    private double amount;

    public Transaction(int id, String category, double amount) {
        this.id = id;
        this.category = category;
        this.amount = amount;
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
}