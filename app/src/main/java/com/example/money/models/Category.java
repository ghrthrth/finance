package com.example.money.models;

public class Category {
    private int id;
    private String name;
    private int color;

    // Конструктор с id, name и color
    public Category(int id, String name, int color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    // Конструктор без id (для создания новой категории)
    public Category(String name, int color) {
        this.name = name;
        this.color = color;
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }
}