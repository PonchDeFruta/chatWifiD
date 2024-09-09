package com.example.chat;

import android.net.wifi.p2p.WifiP2pInfo;
import android.util.Log;

import java.io.OutputStream;
import java.net.Socket;

public class ClientThread extends Thread {
    private static final String TAG = "ClientThread";
    private WifiP2pInfo info;
    private String message;

    public ClientThread(WifiP2pInfo info, String message) {
        this.info = info;
        this.message = message;
    }

    @Override
    public void run() {
        Socket socket = null;
        try {
            socket = new Socket(info.groupOwnerAddress.getHostAddress(), 8888);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write((message + "\n").getBytes());
            outputStream.flush();
            outputStream.close();
            socket.close();
            Log.d(TAG, "Mensaje enviado: " + message);
        } catch (Exception e) {
            Log.e(TAG, "Error al enviar mensaje", e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error al cerrar socket", e);
                }
            }
        }
    }
}
