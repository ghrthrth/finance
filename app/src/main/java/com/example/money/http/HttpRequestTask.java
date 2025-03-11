package com.example.money.http;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/*j4%O8ltioxHk*/


public class HttpRequestTask extends AsyncTask<Void, Void, String> {

    private final String url;
    private final Map<String, String> params;
    private final String method;
    private final HttpRequestCallback callback;

    public HttpRequestTask(Context context, String url, Map<String, String> params, String method, HttpRequestCallback callback) {
        this.url = url;
        this.params = params;
        this.method = method;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            // Create the JSON object
            JSONObject json = new JSONObject();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }

            // Convert the JSON object to a string
            String jsonData = json.toString();

            // Create the request body
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonData);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url);

            switch (method) {
                case "POST":
                    requestBuilder.post(requestBody);
                    break;
                case "PUT":
                    requestBuilder.put(requestBody);
                    break;
                case "DELETE":
                    requestBuilder.delete(requestBody);
                    break;
                case "GET":
                default:
                    requestBuilder.get();
                    break;
            }

            Request request = requestBuilder.build();

            OkHttpClient client = new OkHttpClient();
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                return "Error: " + response.code() + " " + response.message();
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null && result.startsWith("Error:")) {
            callback.onFailure(result);
        } else {
            callback.onSuccess(result);
        }
    }
}
