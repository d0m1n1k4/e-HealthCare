package com.example.auth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.content.Intent;

public class HeartRateMeasurementActivity extends Activity {
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_measurement);

        // Ustawiamy nagłówek
        TextView headerTextView = findViewById(R.id.headerTextView);
        headerTextView.setText("Wykonaj pomiar tetna");

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Tutaj dodaj kod do obsługi powrotu do dashboardu głównego
                // Na przykład, możesz użyć Intent do przejścia do dashboardu głównego.
                Intent intent = new Intent(HeartRateMeasurementActivity.this, DashboardActivity.class);
                startActivity(intent);
            }
        });
    }
}
