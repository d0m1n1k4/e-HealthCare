package com.example.auth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Menu extends Activity {
    private Button logout, btON, manualButton, bleButton, resultGlucoseButton, resultHeartButton, downloadButton, disableBluetoothButton;

    private TextView dashboardTextView;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;

    private BluetoothAdapter myBluetoothAdapter; // Deklaracja zmiennej myBluetoothAdapter do zarządzania funkcjami Bluetooth
    private boolean isBluetoothEnabled = false;

    Intent btEnablingIntent; // Deklaracja zmiennej btEnablingIntent przechowującej intencję do włączenia Bluetooth.
    int requestCodeForEnable; // Deklaracja zmiennej requestCodeForEnable przechowującej kod żądania aktywacji Bluetooth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);

        dashboardTextView = findViewById(R.id.dashboardTextView);
        // Pobranie nazwy użytkownika (człon przed "@") po zalogowaniu
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userEmail = user.getEmail();
        String[] emailParts = userEmail.split("@");
        String username = emailParts[0];
        String welcomeText = "Zalogowany użytkownik: " + username;
        dashboardTextView.setText(welcomeText);

        btON = findViewById(R.id.btON);
        disableBluetoothButton = findViewById(R.id.disableBluetoothButton);
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btEnablingIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        requestCodeForEnable = 1;

        // Sprawdzanie stanu włączenia Bluetooth
        if (myBluetoothAdapter != null && myBluetoothAdapter.isEnabled()) {
            isBluetoothEnabled = true;
            updateBluetoothButtonState();
        }

        bluetoothONMethod();

        bleButton = findViewById(R.id.bleButton);
        bleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu.this, BLEMeasure.class);
                startActivity(intent);
            }
        });

        manualButton = findViewById(R.id.manualButton);
        manualButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu.this, ManualMeasure.class);
                startActivity(intent);
            }
        });

        resultGlucoseButton = findViewById(R.id.resultGlucoseButton);
        resultGlucoseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu.this, GlucoseAnalysis.class);
                startActivity(intent);
            }
        });

        resultHeartButton = findViewById(R.id.resultHeartButton);
        resultHeartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu.this, HeartAnalysis.class);
                startActivity(intent);
            }
        });

        downloadButton = findViewById(R.id.downloadButton);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu.this, DownloadResults.class);
                startActivity(intent);
            }
        });


        disableBluetoothButton = findViewById(R.id.disableBluetoothButton);
        disableBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableBluetooth();
            }
        });

        logout = findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(Menu.this, SignIn.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void disableBluetooth() {
        if (myBluetoothAdapter != null && myBluetoothAdapter.isEnabled()) {
            myBluetoothAdapter.disable();
            isBluetoothEnabled = false;
            updateBluetoothButtonState();
            Toast.makeText(getApplicationContext(), "Bluetooth został wyłączony", Toast.LENGTH_LONG).show();
        }
    }


    private void bluetoothONMethod() {
        btON.setEnabled(!isBluetoothEnabled);
        btON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBluetoothEnabled) {
                    // Jeżeli Bluetooth jest już włączony, wyświetli się komunikat
                    Toast.makeText(getApplicationContext(), "Bluetooth jest już włączony", Toast.LENGTH_LONG).show();
                } else {
                    if (myBluetoothAdapter != null && !myBluetoothAdapter.isEnabled()) {
                        startActivityForResult(btEnablingIntent, requestCodeForEnable);
                    }
                }
            }
        });
    }


    private void updateBluetoothButtonState() {
        if (isBluetoothEnabled) {
            btON.setEnabled(false);
        } else {
            btON.setText("Uruchom Bluetooth");
            btON.setEnabled(true);
        }
    }


}