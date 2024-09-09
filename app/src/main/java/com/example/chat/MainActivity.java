package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 1;

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;

    private RecyclerView peersRecyclerView;
    private PeerListAdapter peerListAdapter;
    private List<WifiP2pDevice> peers = new ArrayList<>();

    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messagesList = new ArrayList<>();

    private EditText messageInput;
    private Button sendButton;
    private Button discoverButton;

    private WifiP2pInfo connectionInfo;
    private ServerThread serverThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 12 o superior
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                // Si el permiso no está concedido, solicítalo
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.NEARBY_WIFI_DEVICES},
                        1001); // Código de solicitud de permiso
            } else {
                // Si ya tienes el permiso, continúa con el descubrimiento de dispositivos
                discoverPeers();
            }
        } else {
            // Si es una versión inferior a Android 12, no se necesita este permiso
            discoverPeers();
        }


        // Inicializar UI
        peersRecyclerView = findViewById(R.id.peersRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        discoverButton = findViewById(R.id.discoverButton);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);

        // Configurar RecyclerView de pares
        peerListAdapter = new PeerListAdapter(peers, new PeerListAdapter.OnPeerClickListener() {
            @Override
            public void onPeerClick(WifiP2pDevice device) {
                connectToDevice(device);
            }
        });
        peersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        peersRecyclerView.setAdapter(peerListAdapter);

        // Configurar RecyclerView de mensajes
        messageAdapter = new MessageAdapter(messagesList);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);

        // Inicializar Wi-Fi Direct
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        // Configurar IntentFilter
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Registrar BroadcastReceiver
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);

        // Solicitar permisos si es necesario
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_CODE);
        }

        // Configurar botón de descubrir pares
        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discoverPeers();
            }
        });

        // Configurar botón de enviar mensaje
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = messageInput.getText().toString();
                if (message.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Escribe un mensaje", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (connectionInfo != null && connectionInfo.groupFormed && connectionInfo.isGroupOwner) {
                    // Si eres el host, enviar directamente
                    messageAdapter.addMessage(new Message(message, true));
                    messageInput.setText("");
                    // Implementar lógica para enviar mensajes
                    // Por ejemplo, mantener una lista de clientes conectados
                } else if (connectionInfo != null && connectionInfo.groupFormed) {
                    // Si eres el cliente, enviar al host
                    messageAdapter.addMessage(new Message(message, true));
                    messageInput.setText("");
                    ClientThread clientThread = new ClientThread(connectionInfo, message);
                    clientThread.start();
                } else {
                    Toast.makeText(MainActivity.this, "No estás conectado a ningún dispositivo", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Iniciar el servidor para recibir mensajes
        serverThread = new ServerThread(new ServerThread.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(String message) {
                messageAdapter.addMessage(new Message(message, false));
            }
        });
        serverThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if (serverThread != null) {
            serverThread.stopServer();
        }
    }

    private void discoverPeers() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 12 o superior
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.NEARBY_WIFI_DEVICES},
                        1001); // Código de solicitud de permiso
                return; // No seguir si el permiso no está concedido
            }
        }

        // Llamar a discoverPeers si se tienen los permisos
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Descubrimiento iniciado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, "Fallo en el descubrimiento: " + reason, Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void updatePeerList(WifiP2pDeviceList peerList) {
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        peerListAdapter.updatePeerList(peers);
        if (peers.isEmpty()) {
            Toast.makeText(this, "No se encontraron dispositivos", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private void connectToDevice(WifiP2pDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 12 o superior
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.NEARBY_WIFI_DEVICES},
                        1001); // Código de solicitud de permiso
                return; // No seguir si el permiso no está concedido
            }
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Conectando a " + device.deviceName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, "Fallo en la conexión: " + reason, Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onConnectionInfoAvailable(@NonNull WifiP2pInfo info) {
        connectionInfo = info;
        if (info.groupFormed && info.isGroupOwner) {
            Toast.makeText(this, "Eres el anfitrión", Toast.LENGTH_SHORT).show();
            // Implementar lógica para aceptar clientes si es necesario
        } else if (info.groupFormed) {
            Toast.makeText(this, "Conectado al host", Toast.LENGTH_SHORT).show();
        }
    }

    // Manejo de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
            } else {
                // Permiso denegado
                Toast.makeText(this, "Permiso denegado. La aplicación no funcionará correctamente.", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
