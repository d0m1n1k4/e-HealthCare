package com.example.auth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public class BLEMeasure extends Activity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_measure);

        Button bleMeasurementBackButton = findViewById(R.id.bleMeasurementBackButton);
        Button connectToRaspberryButton = findViewById(R.id.connectToRaspberryButton);

        // Inicjalizacja adaptera Bluetooth
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        bleMeasurementBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Powrót do DashboardActivity
                Intent intent = new Intent(BLEMeasure.this, Menu.class);
                startActivity(intent);
            }
        });

        connectToRaspberryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connected) {
                    // Jeśli aplikacja jest już połączona, połączenie rozłączy się
                    disconnectFromRaspberry();
                } else {
                    // Jeśli aplikacja nie jest połączona, spróbuj nawiązać połączenie
                    connectToRaspberry();
                }
            }
        });
    }

    // Funkcja do nawiązywania połączenia z Raspberry Pi Pico
    private void connectToRaspberry() {
        if (bluetoothAdapter != null) {
            // Sprawdzanie, czy Bluetooth jest włączony
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivity(enableBtIntent);
                return;
            }

            // Szukanie Raspberry Pi Pico (na podstawie MAC)
            String raspberryMacAddress = "28:CD:C1:03:EB:9F"; // Identyfikator MAC Raspberry Pi Pico
            BluetoothDevice raspberryDevice = bluetoothAdapter.getRemoteDevice(raspberryMacAddress);

            if (raspberryDevice != null) {
                // Nawiązywanie połączenie z Raspberry Pi Pico
                bluetoothGatt = raspberryDevice.connectGatt(this, false, gattCallback);
            } else {
                Toast.makeText(this, "Nie można znaleźć urządzenia Raspberry Pi Pico", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Funkcja do rozłączania z Raspberry Pi Pico
    private void disconnectFromRaspberry() {
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bluetoothGatt.disconnect();
        }
    }

    // Callback dla BluetoothGatt
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected = true;
                Toast.makeText(BLEMeasure.this, "Połączono z Raspberry Pi Pico", Toast.LENGTH_SHORT).show();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connected = false;
                Toast.makeText(BLEMeasure.this, "Rozłączono z Raspberry Pi Pico", Toast.LENGTH_SHORT).show();
            }
        }
    };
}
