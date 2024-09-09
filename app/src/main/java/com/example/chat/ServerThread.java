package com.example.chat;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerThread extends Thread {
    private static final String TAG = "ServerThread";
    private boolean running = true;
    private Handler handler;

    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }

    private OnMessageReceivedListener listener;

    public ServerThread(OnMessageReceivedListener listener) {
        this.listener = listener;
        handler = new Handler(Looper.getMainLooper());
    }

    public void stopServer() {
        running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(8888);
            Log.d(TAG, "Servidor iniciado en el puerto 8888");
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar servidor", e);
            return;
        }

        while (running) {
            try {
                Socket client = serverSocket.accept();
                Log.d(TAG, "Cliente conectado: " + client.getInetAddress());
                InputStream inputStream = client.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String message = reader.readLine();
                Log.d(TAG, "Mensaje recibido: " + message);
                if (message != null && listener != null) {
                    handler.post(() -> listener.onMessageReceived(message));
                }
                reader.close();
                client.close();
            } catch (Exception e) {
                if (running) {
                    Log.e(TAG, "Error en el servidor", e);
                }
            }
        }

        try {
            serverSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "Error al cerrar servidor", e);
        }
    }
}
