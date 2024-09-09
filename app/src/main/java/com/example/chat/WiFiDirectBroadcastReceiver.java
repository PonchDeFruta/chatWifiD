package com.example.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WiFiDirectBroadcastReceiver";

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private MainActivity activity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, MainActivity activity) {
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Estado de Wi-Fi Direct cambiado
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Toast.makeText(context, "Wi-Fi Direct habilitado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Wi-Fi Direct deshabilitado", Toast.LENGTH_SHORT).show();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Lista de pares actualizada
            if (manager != null) {
                // Verificar si tenemos el permiso de dispositivos cercanos en Android 12 o superior
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.NEARBY_WIFI_DEVICES)
                        == PackageManager.PERMISSION_GRANTED) {
                    manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                        @Override
                        public void onPeersAvailable(WifiP2pDeviceList peerList) {
                            activity.updatePeerList(peerList);
                        }
                    });
                } else {
                    // Si no tienes el permiso, muestra un mensaje o maneja el caso en la actividad principal
                    Toast.makeText(context, "Permiso NEARBY_WIFI_DEVICES no concedido", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Conexión cambiada
            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                // Conectado a un grupo
                manager.requestConnectionInfo(channel, activity);
            } else {
                // Desconectado
                Toast.makeText(context, "Desconectado", Toast.LENGTH_SHORT).show();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Información del dispositivo actual
        }
    }
}
