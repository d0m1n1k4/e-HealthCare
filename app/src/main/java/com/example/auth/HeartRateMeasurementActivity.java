package com.example.auth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class HeartRateMeasurementActivity extends Activity {
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_measurement);

        // Ustawiamy nagłówek
        TextView headerTextView = findViewById(R.id.headerTextView);
        headerTextView.setText("Wykonaj pomiar tetna");
    }
}