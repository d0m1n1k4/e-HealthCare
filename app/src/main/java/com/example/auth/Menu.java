package com.example.auth;

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
    private Button logout, btON, manualButton, bleButton, resultGlucoseButton, resultHeartButton, downloadButton;

    private TextView dashboardTextView;
    private BluetoothAdapter myBluetoothAdapter; //Deklaracja zmiennej myBluetoothAdapter do zarządzania funkcjami Bluetooth

    Intent btEnablingIntent; //Deklaracja zmiennej btEnablingIntent przechowującej intencję do włączenia Bluetooth.
    int requestCodeForEnable; //Deklaracja zmiennej requestCodeForEnable przechowującej kod żądania aktywacji Bluetooth


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

        btON = (Button) findViewById(R.id.btON);
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btEnablingIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        requestCodeForEnable = 1;

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
                Intent intent = new Intent(Menu.this, MeasureManager.class);
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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode==requestCodeForEnable)
        {
            if(resultCode==RESULT_OK)
            {
                Toast.makeText(getApplicationContext(), "Bluetooth is Enable",Toast.LENGTH_LONG).show();
            }else if (resultCode==RESULT_CANCELED)
            {
                Toast.makeText(getApplicationContext(), "Bluetooth Enabling Cancelled", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void bluetoothONMethod() {
        btON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(myBluetoothAdapter==null)
                {
                    Toast.makeText(getApplicationContext(), "Bluetooth is not supported on this Device", Toast.LENGTH_LONG).show();
                }else {
                    if(!myBluetoothAdapter.isEnabled())
                    {
                        startActivityForResult(btEnablingIntent,requestCodeForEnable);

                    }
                }
            }
        });
    }
}