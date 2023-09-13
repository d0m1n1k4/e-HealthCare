package com.example.auth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HeartRateMeasurementActivity extends Activity {

    private TextView headerTextView;
    private RecyclerView recyclerView;
    private MeasurementAdapter adapter;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_measurement);

        // Ustawiamy nagłówek
        headerTextView = findViewById(R.id.headerTextView);
        headerTextView.setText("Wykonaj pomiar tetna");

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Przygotowujemy dane do wyświetlenia w liście pomiarów
        List<String> measurements = new ArrayList<>();
        measurements.add("Pomiar 1.");
        measurements.add("Pomiar 2.");
        measurements.add("Pomiar 3.");
        measurements.add("Pomiar 4.");
        measurements.add("Pomiar 5.");

        // Inicjalizujemy adapter i przypisujemy go do RecyclerView
        adapter = new MeasurementAdapter(measurements);
        recyclerView.setAdapter(adapter);

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





