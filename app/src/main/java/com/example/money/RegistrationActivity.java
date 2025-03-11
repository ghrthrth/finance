package com.example.money;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.money.http.HttpRequestCallback;
import com.example.money.http.HttpRequestTask;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration_activity);

        Button send_reg_data = findViewById(R.id.send_reg_data);

        EditText inputName = findViewById(R.id.input_name);
        EditText inputLogin = findViewById(R.id.input_login_reg);
        EditText inputPassword = findViewById(R.id.input_pass_reg);
        CheckBox agreement = findViewById(R.id.checkBox);

        send_reg_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = inputName.getText().toString();
                String login = inputLogin.getText().toString();
                String password = inputPassword.getText().toString();
                boolean isChecked = agreement.isChecked();

                if (name.isEmpty() || login.isEmpty() || password.isEmpty() || !isChecked) {
                    Toast.makeText(RegistrationActivity.this, "Please fill in all the fields and check the agreement", Toast.LENGTH_SHORT).show();
                } else {
                    // Создаем параметры для отправки на сервер
                    Map<String, String> params = new HashMap<>();
                    params.put("name", name);
                    params.put("login", login);
                    params.put("password", password);

                    // Создаем и выполняем задачу HTTP-запроса
                    HttpRequestTask task = new HttpRequestTask(
                            RegistrationActivity.this,
                            "https://claimbes.store/spend_smart/api/registration.php",
                            params,
                            "POST",
                            new HttpRequestCallback() {
                                @Override
                                public void onSuccess(String response) {
                                    // Обработка успешного ответа
                                    Toast.makeText(RegistrationActivity.this, "Registration successful: " + response, Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }

                                @Override
                                public void onFailure(String error) {
                                    // Обработка ошибки
                                    Toast.makeText(RegistrationActivity.this, "Registration failed: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });

                    task.execute();
                }
            }
        });
    }

}