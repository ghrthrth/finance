package com.example.money.ui.home;

public class Transaction {
    private String category;
    private double amount;

    private Transaction(Builder builder) {
        this.category = builder.category;
        this.amount = builder.amount;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public static class Builder {
        private String category;
        private double amount;

        public Builder setCategory(String category) {
            this.category = category;
            return this;
        }

        public Builder setAmount(double amount) {
            this.amount = amount;
            return this;
        }

        public Transaction build() {
            return new Transaction(this);
        }
    }
}
