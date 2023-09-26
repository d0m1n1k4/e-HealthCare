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

public class DashboardActivity extends Activity {
    private Button logout, btON, hrButton;

    private TextView dashboardTextView;
    private BluetoothAdapter myBluetoothAdapter; //Deklaracja zmiennej myBluetoothAdapter do zarządzania funkcjami Bluetooth

    Intent btEnablingIntent; //Deklaracja zmiennej btEnablingIntent przechowującej intencję do włączenia Bluetooth.
    int requestCodeForEnable; //Deklaracja zmiennej requestCodeForEnable przechowującej kod żądania aktywacji Bluetooth


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        dashboardTextView = findViewById(R.id.dashboardTextView);
        // Pobranie nazwy użytkownika (człon przed "@") po zalogowaniu
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userEmail = user.getEmail();
        String[] emailParts = userEmail.split("@");
        String username = emailParts[0];
        String welcomeText = "Witaj " + username;
        dashboardTextView.setText(welcomeText);

        btON = (Button) findViewById(R.id.btON);
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btEnablingIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //Stworzenie intencji aktywacji Bluetooth.
        requestCodeForEnable = 1;

        bluetoothONMethod();

        hrButton = findViewById(R.id.hrButton); // Dodaj tę linię
        hrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Otwieranie nowego Dashboard zatytułowanego "Pomiary ręczne"
                Intent intent = new Intent(DashboardActivity.this, HeartRateMeasurementActivity.class);
                startActivity(intent);
            }
        });

        logout = findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
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
        btON.setOnClickListener(new View.OnClickListener() { //Ustawienie nasłuchiwacza na kliknięcie przycisku "btON". Jeśli urządzenie obsługuje Bluetooth, a Bluetooth nie jest włączony, zostanie uruchomione okno dialogowe w celu włączenia Bluetooth.
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
