package com.example.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class BLEMeasure extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_measurement);


        Button bleMeasurementBackButton = findViewById(R.id.bleMeasurementBackButton);
        bleMeasurementBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Wróć do DashboardActivity
                Intent intent = new Intent(BLEMeasure.this, Menu.class);
                startActivity(intent);
            }
        });
    }
}