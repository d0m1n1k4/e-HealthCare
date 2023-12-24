package com.example.auth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class Menu extends Activity {


    private static final String TAG = "Menu";
    /**
     * Deklaracja zmiennej requestCodeForEnable przechowującej kod żądania aktywacji Bluetooth.
     */
    private int requestCodeForEnable = 1;
    private Button logout, btON, manualButton, bleButton, resultGlucoseButton, resultHeartButton, downloadButton, disableBluetoothButton;
    private TextView dashboardTextView;
    /**
     * Deklaracja zmiennej myBluetoothAdapter do zarządzania funkcjami Bluetooth.
     */
    private BluetoothAdapter myBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);

        bleButton = findViewById(R.id.bleButton);
        btON = findViewById(R.id.btON);
        dashboardTextView = findViewById(R.id.dashboardTextView);
        disableBluetoothButton = findViewById(R.id.disableBluetoothButton);
        downloadButton = findViewById(R.id.downloadButton);
        logout = findViewById(R.id.logout);
        manualButton = findViewById(R.id.manualButton);
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        resultGlucoseButton = findViewById(R.id.resultGlucoseButton);
        resultHeartButton = findViewById(R.id.resultHeartButton);

        updateBluetoothButtonState(isBluetoothEnabled());

        btON.setOnClickListener(view -> {
            if (isBluetoothEnabled()) {
                Toast.makeText(getApplicationContext(), "Bluetooth jest już włączony", Toast.LENGTH_LONG).show();
            } else {
                if (!isBluetoothEnabled()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        if (requestForPermissions(new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}))
                            return;

                        requestEnableBluetooth();
                    } else {
                        requestEnableBluetooth();
                    }
                }
            }
        });

        // Pobranie nazwy użytkownika (człon przed "@") po zalogowaniu
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userEmail = user.getEmail();
        String[] emailParts = userEmail.split("@");
        String username = emailParts[0];
        String welcomeText = "Zalogowany użytkownik: " + username;
        dashboardTextView.setText(welcomeText);

        bleButton.setOnClickListener(v -> {
            Intent intent = new Intent(Menu.this, BLEMeasure.class);
            startActivity(intent);
        });

        manualButton.setOnClickListener(v -> {
            Intent intent = new Intent(Menu.this, ManualMeasure.class);
            startActivity(intent);
        });

        resultGlucoseButton.setOnClickListener(v -> {
            Intent intent = new Intent(Menu.this, GlucoseAnalysis.class);
            startActivity(intent);
        });

        resultHeartButton.setOnClickListener(v -> {
            Intent intent = new Intent(Menu.this, HeartAnalysis.class);
            startActivity(intent);
        });

        downloadButton.setOnClickListener(v -> {
            Intent intent = new Intent(Menu.this, DownloadResults.class);
            startActivity(intent);
        });


        disableBluetoothButton.setOnClickListener(v -> disableBluetooth());

        logout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Menu.this, SignIn.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == requestCodeForEnable) {
            for (int i = 0; i < grantResults.length; i++) {
                int grantResult = grantResults[i];

                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "onRequestPermissionsResult: " + permissions[i] + " not GRANTED! ABORTING!");
                    return;
                }

                requestEnableBluetooth();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressLint("MissingPermission")
    private void requestEnableBluetooth() {
        startActivityForResult(
                new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                requestCodeForEnable
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCodeForEnable == requestCode) {
            updateBluetoothButtonState(isBluetoothEnabled());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private boolean isBluetoothEnabled() {
        return myBluetoothAdapter != null && myBluetoothAdapter.isEnabled();
    }

    @SuppressLint("MissingPermission")
    private void disableBluetooth() {
        if (isBluetoothEnabled()) {
            myBluetoothAdapter.disable();
            updateBluetoothButtonState(isBluetoothEnabled());
            Toast.makeText(getApplicationContext(), "Bluetooth został wyłączony", Toast.LENGTH_LONG).show();
        }
    }

    private void updateBluetoothButtonState(boolean bluetoothEnabled) {
        if (bluetoothEnabled) {
            btON.setEnabled(false);
            bleButton.setEnabled(true);
        } else {
            btON.setEnabled(true);
            bleButton.setEnabled(false);
        }
    }

    private boolean requestForPermissions(String[] permissions1) {
        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : permissions1) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 1);
            // Zapytanie o uprawnienia BLE
            return true;
        }

        return false;
    }

}