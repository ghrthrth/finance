package com.example.money;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.money.http.HttpRequestCallback;
import com.example.money.http.HttpRequestTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity); // Убедитесь, что у вас есть layout-файл login_activity.xml

        Button loginButton = findViewById(R.id.login_button);
        EditText inputLogin = findViewById(R.id.input_login);
        EditText inputPassword = findViewById(R.id.input_password);

        Button goToRegistration = findViewById(R.id.go_to_registration);
        goToRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String login = inputLogin.getText().toString();
                String password = inputPassword.getText().toString();

                if (login.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please fill in all the fields", Toast.LENGTH_SHORT).show();
                } else {
                    // Создаем параметры для отправки на сервер
                    Map<String, String> params = new HashMap<>();
                    params.put("login", login);
                    params.put("password", password);

                    // Логируем параметры
                    Log.d("LoginParams", "Login: " + login + ", Password: " + password);

                    // Создаем и выполняем задачу HTTP-запроса
                    HttpRequestTask task = new HttpRequestTask(
                            LoginActivity.this,
                            "https://claimbes.store/spend_smart/api/login.php",
                            params,
                            "POST",
                            new HttpRequestCallback() {
                                @Override
                                public void onSuccess(String response) {
                                    Log.d("LoginResponse", "Response: " + response);

                                    try {
                                        // Парсим JSON-ответ от сервера
                                        JSONObject jsonResponse = new JSONObject(response);

                                        // Проверяем, есть ли ошибка в ответе
                                        if (jsonResponse.has("error")) {
                                            // Если есть ошибка, показываем сообщение
                                            String errorMessage = jsonResponse.getString("error");
                                            Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                                        } else {
                                            // Если ошибки нет, извлекаем user_id из JSON
                                            int userId = jsonResponse.getInt("user_id");

                                            // Сохраняем состояние авторизации и user_id в SharedPreferences
                                            SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.putBoolean("is_logged_in", true);
                                            editor.putInt("user_id", userId); // Сохраняем user_id
                                            editor.apply();

                                            // Переходим на MainActivity
                                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    } catch (JSONException e) {
                                        // Обработка ошибки парсинга JSON
                                        e.printStackTrace();
                                        Toast.makeText(LoginActivity.this, "Login failed: Invalid server response", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(String error) {
                                    // Обработка ошибки сети или сервера
                                    Log.e("LoginError", "Error: " + error);
                                    Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });

                    task.execute();
                }
            }
        });
    }
}