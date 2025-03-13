package com.example.money.http;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;



/*j4%O8ltioxHk*/


public class HttpRequestTask extends AsyncTask<Void, Void, String> {
    private Context context;
    private String url;
    private Map<String, String> params;
    private String method;
    private HttpRequestCallback callback;

    public HttpRequestTask(Context context, String url, Map<String, String> params, String method, HttpRequestCallback callback) {
        this.context = context;
        this.url = url;
        this.params = params;
        this.method = method;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Void... voids) {
        HttpURLConnection connection = null;
        try {
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);

            // Формируем JSON
            JSONObject jsonParam = new JSONObject();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                jsonParam.put(entry.getKey(), entry.getValue());
            }

            // Отправляем данные
            OutputStream os = connection.getOutputStream();
            os.write(jsonParam.toString().getBytes("UTF-8"));
            os.close();

            // Получаем ответ
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            } else {
                return "Error: " + responseCode;
            }
        } catch (Exception e) {
            Log.e("HttpRequestTask", "Error: " + e.getMessage());
            return "Error: " + e.getMessage();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result.startsWith("Error:")) {
            callback.onFailure(result);
        } else {
            callback.onSuccess(result);
        }
    }
}