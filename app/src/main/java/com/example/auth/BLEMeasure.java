package com.example.auth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.UUID;

public class BLEMeasure extends Activity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private boolean connected = false;

    private static final String RASPBERRY_ADDRESS = "28:CD:C1:03:EB:9F";
    private static final UUID MOBILE_APP_SERVICE_UUID = UUID.fromString("c539cece-97a4-11ee-b9d1-0242ac120002");
    private static final UUID MOBILE_APP_CHAR_UUID = UUID.fromString("c539cece-97a4-11ee-b9d1-0242ac120002");

    private Handler scanHandler = new Handler();
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_measure);

        Button bleMeasurementBackButton = findViewById(R.id.bleMeasurementBackButton);
        Button connectToRaspberryButton = findViewById(R.id.connectToRaspberryButton);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        bleMeasurementBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BLEMeasure.this, Menu.class);
                startActivity(intent);
            }
        });

        connectToRaspberryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connected) {
                    disconnectFromRaspberry();
                } else {
                    startScan();
                }
            }
        });

        startScan();
    }

    private void startScan() {
        Log.d("BLEMeasure", "Starting Bluetooth LE scan...");
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(BLEMeasure.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(BLEMeasure.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    return;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                scanHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bluetoothAdapter.stopLeScan(leScanCallback);
                        Log.d("BLEMeasure", "Bluetooth LE scan stopped.");
                    }
                }, SCAN_PERIOD);

                bluetoothAdapter.startLeScan(leScanCallback);
                Log.d("BLEMeasure", "Bluetooth LE scan started successfully.");
            }
        } else {
            Log.e("BLEMeasure", "Bluetooth is not enabled.");
        }
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (ActivityCompat.checkSelfPermission(BLEMeasure.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    Log.d("BLEMeasure", "Found device: " + device.getName() + ", Address: " + device.getAddress());
                    if (RASPBERRY_ADDRESS.equals(device.getAddress())) {
                        Log.d("BLEMeasure", "Found Raspberry Pi Pico. Connecting...");
                        connectToRaspberry(device);
                        scanHandler.removeCallbacksAndMessages(null);
                    }
                }
            });
        }
    };

    private void connectToRaspberry(BluetoothDevice raspberryDevice) {
        if (bluetoothAdapter == null || raspberryDevice == null) {
            Log.e("BLEMeasure", "BluetoothAdapter or RaspberryDevice is null");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e("BLEMeasure", "Bluetooth is not enabled");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BLEMeasure", "Bluetooth connect permission not granted");
                return;
            }
        }

        // zabezpieczenie przed ponownym próbowaniem połączenia, gdy już jesteśmy połączeni
        if (connected) {
            Log.d("BLEMeasure", "Already connected to Raspberry Pi Pico");
            return;
        }

        // zabezpieczenie przed próbą ponownego połączenia w trakcie trwającego skanowania
        scanHandler.removeCallbacksAndMessages(null);

        Log.d("BLEMeasure", "Connecting to Raspberry Pi Pico...");
        bluetoothGatt = raspberryDevice.connectGatt(BLEMeasure.this, false, gattCallback);
    }
    private void disconnectFromRaspberry() {
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothGatt.disconnect();
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        connected = true;
                        Toast.makeText(BLEMeasure.this, "Connected to Raspberry Pi Pico", Toast.LENGTH_SHORT).show();
                        Log.d("BLEMeasure", "Connected to Raspberry Pi Pico");

                        BluetoothGattService service = gatt.getService(MOBILE_APP_SERVICE_UUID);
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(MOBILE_APP_CHAR_UUID);
                        if (ActivityCompat.checkSelfPermission(BLEMeasure.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            gatt.readCharacteristic(characteristic);
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
                    {
                        connected = false;
                        Toast.makeText(BLEMeasure.this, "Disconnected from Raspberry Pi Pico", Toast.LENGTH_SHORT).show();
                        Log.d("BLEMeasure", "Disconnected from Raspberry Pi Pico");
                    }
                }
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] value = characteristic.getValue();
                Log.d("BLEMeasure", "Read characteristic value: " + new String(value));
            } else {
                Log.e("BLEMeasure", "Characteristic read failed with status: " + status);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
    }

    private void stopScan() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bluetoothAdapter.stopLeScan(leScanCallback);
                Log.d("BLEMeasure", "Bluetooth LE scan stopped.");
            }
        }
    }
}