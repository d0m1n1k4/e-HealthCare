package com.example.auth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
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
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class BLEMeasure extends Activity {

    private static final String RASPBERRY_ADDRESS = "28:CD:C1:03:EB:9F";
    private static final UUID HEART_RATE_SERVICE_UUID = UUID.fromString("00001808-0000-1000-8000-00805f9b34fb");
    private static final UUID HEART_RATE_CHAR_UUID = UUID.fromString("00002A18-0000-1000-8000-00805f9b34fb");

    private final String[] permissions = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN
    };

    private List<String> permissionsToRequest;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private String selectedDate = "";
    private TextView selectedDateTextView;

    private TextView measurementValueTextView;

    private static final long SCAN_PERIOD = 10000;

    private static final String TAG = "BLEMeasure";

    private boolean connectedWithDevice = false;

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            runOnUiThread(() -> {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectedWithDevice = true;
                    Log.d("BLEMeasure", "Connected to Raspberry Pi Pico");
                    // Odkrywanie usług po połączeniu
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    connectedWithDevice = false;
                    Log.d("BLEMeasure", "Disconnected from Raspberry Pi Pico");
                }
            });
        }

        //Metoda wywoływana, gdy usługi BLE są odkrywane na Raspberry
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //sprawdzenie, czy odkrywanie GATT zakończyło się sukcesem
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //pobieranie charakterystyki UUID dla tętna
                BluetoothGattService service = gatt.getService(HEART_RATE_SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(HEART_RATE_CHAR_UUID);
                    if (characteristic != null) {
                        if (ActivityCompat.checkSelfPermission(BLEMeasure.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        gatt.readCharacteristic(characteristic);
                    } else {
                        Log.e("BLEMeasure", "Heart Rate Characteristic not found");
                    }
                } else {
                    Log.e("BLEMeasure", "Heart Rate Service not found");
                }
            }
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

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d("BLEMeasure", "Found device: " + device.getName() + ", Address: " + device.getAddress());

            if (RASPBERRY_ADDRESS.equals(device.getAddress())) {
                Log.d("BLEMeasure", "Found Raspberry Pi Pico. Connecting...");
                stopScan();
                connectToRaspberry(device);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_measure);

        Button bleMeasurementBackButton = findViewById(R.id.bleMeasurementBackButton);
        measurementValueTextView = findViewById(R.id.measurementValueTextView);
        Button heart1Button = findViewById(R.id.heart1Button);
        Button connectToRaspberryButton = findViewById(R.id.connectToRaspberryButton);
        Button datePickerButton = findViewById(R.id.datePickerButton);
        selectedDateTextView = findViewById(R.id.selectedDateTextView);
        datePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        bleMeasurementBackButton.setOnClickListener(v -> {
            Intent intent = new Intent(BLEMeasure.this, Menu.class);
            startActivity(intent);
        });

        heart1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                measurementValueTextView.setText("125");
                measurementValueTextView.setTextColor(Color.BLACK);
                measurementValueTextView.setVisibility(View.VISIBLE);
            }
        });


        connectToRaspberryButton.setOnClickListener(v -> {
            if (connectedWithDevice) {
                disconnectBluetoothGatt();
            } else {
                startScan();
            }
        });



        startScan();


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            for (int i = 0; i < grantResults.length; i++) {
                int grantResult = grantResults[i];

                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "onRequestPermissionsResult: " + permissions[i] + " not GRANTED! ABORTING!");
                    return;
                }
            }

            restartBLE();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void startScan() {
        Log.d("BLEMeasure", "Starting Bluetooth LE scan...");

        if (isBluetoothEnabled()) {
            if (requestForPermissions(permissions)) {
                return;
            }

            restartBLE();
        } else {
            Log.e("BLEMeasure", "Bluetooth is not enabled.");
        }
    }

    private boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    private boolean requestForPermissions(String[] permissions1) {
        permissionsToRequest = new ArrayList<>();

        for (String permission : permissions1) {
            if (ActivityCompat.checkSelfPermission(BLEMeasure.this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            String bluetoothScan = android.Manifest.permission.BLUETOOTH_SCAN;
            if (ActivityCompat.checkSelfPermission(this, bluetoothScan) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(bluetoothScan);
            }
            String bluetoothConnect = Manifest.permission.BLUETOOTH_CONNECT;
            if (ActivityCompat.checkSelfPermission(this, bluetoothConnect) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(bluetoothConnect);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(BLEMeasure.this, permissionsToRequest.toArray(new String[0]), 1);
            return true;
        }

        return false;
    }

    @SuppressLint("MissingPermission")
    private void restartBLE() {
        Log.d("BLEMeasure", "Bluetooth LE stop scan.");
        bluetoothAdapter.stopLeScan(leScanCallback);

        Log.d("BLEMeasure", "Bluetooth LE start scan.");
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    @SuppressLint("MissingPermission")
    private void connectToRaspberry(BluetoothDevice raspberryDevice) {
        if (bluetoothAdapter == null || raspberryDevice == null) {
            Log.e("BLEMeasure", "BluetoothAdapter or RaspberryDevice is null");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e("BLEMeasure", "Bluetooth is not enabled");
            return;
        }

        // Zabezpieczenie przed próbą ponownego połączenia, gdy jesteśmy już połączeni
        if (connectedWithDevice) {
            Log.d("BLEMeasure", "Already connected to Raspberry Pi Pico");
            return;
        }

        Log.d("BLEMeasure", "Connecting to Raspberry Pi Pico...");
        bluetoothGatt = raspberryDevice.connectGatt(BLEMeasure.this, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    @SuppressLint("MissingPermission")
    private void disconnectBluetoothGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
    }

    @SuppressLint("MissingPermission")
    private void stopScan() {
        if (isBluetoothEnabled()) {
            bluetoothAdapter.stopLeScan(leScanCallback);
            Log.d("BLEMeasure", "Bluetooth LE scan stopped.");
        }
    }

    private void showDatePickerDialog() {
        // Pobieranie aktualnej daty
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                // Aktualizacja wybranej daty
                selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
                selectedDateTextView.setText("Data pomiaru: " + selectedDate);
            }
        }, year, month, day);
        datePickerDialog.show();
    }
}
