package com.example.money.http;

public interface HttpRequestCallback {
    void onSuccess(String response);
    void onFailure(String error);
}
