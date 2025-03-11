package com.example.money.webSocket;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketClient extends WebSocketListener {

    private WebSocket webSocket;
    private final String serverUrl; // URL вашего WebSocket-сервера
    private final WebSocketCallback callback; // Интерфейс для обратных вызовов

    public WebSocketClient(String serverUrl, WebSocketCallback callback) {
        this.serverUrl = serverUrl;
        this.callback = callback;
    }

    // Метод для подключения к серверу
    public void connect() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(serverUrl).build();
        webSocket = client.newWebSocket(request, this);
    }

    // Метод для отправки сообщения
    public void sendMessage(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        }
    }

    // Метод для закрытия соединения
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing connection");
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, okhttp3.Response response) {
        super.onOpen(webSocket, response);
        callback.onWebSocketOpen(); // Уведомляем о подключении
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        callback.onMessageReceived(text); // Уведомляем о получении сообщения
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        super.onMessage(webSocket, bytes);
        callback.onMessageReceived(bytes.utf8()); // Уведомляем о получении бинарного сообщения
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        super.onClosing(webSocket, code, reason);
        callback.onWebSocketClosing(code, reason); // Уведомляем о закрытии соединения
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
        super.onFailure(webSocket, t, response);
        callback.onWebSocketFailure(t); // Уведомляем об ошибке
    }

    // Интерфейс для обратных вызовов
    public interface WebSocketCallback {
        void onWebSocketOpen();
        void onMessageReceived(String message);
        void onWebSocketClosing(int code, String reason);
        void onWebSocketFailure(Throwable t);
    }
}
