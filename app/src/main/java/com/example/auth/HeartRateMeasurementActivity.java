package com.example.auth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.util.Log;

public class HeartRateMeasurementActivity extends Activity {

    private TextView headerTextView;
    private RecyclerView recyclerView;
    private MeasurementAdapter adapter;
    private FirebaseAuth firebaseAuth;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_measurement);

        // Inicjalizacja Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // Ustawiamy nagłówek
        headerTextView = findViewById(R.id.headerTextView);
        headerTextView.setText("Pomiary ręczne");

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Przygotowujemy dane do wyświetlenia w liście pomiarów
        List<Measurement> measurements = new ArrayList<>();
        measurements.add(new Measurement("Pomiar 1", "", ""));
        measurements.add(new Measurement("Pomiar 2", "", ""));
        measurements.add(new Measurement("Pomiar 3", "", ""));
        measurements.add(new Measurement("Pomiar 4", "", ""));
        measurements.add(new Measurement("Pomiar 5", "", ""));

        // Inicjalizujemy adapter i przypisujemy go do RecyclerView
        adapter = new MeasurementAdapter(measurements);
        recyclerView.setAdapter(adapter);

        // Przycisk czyszczący wprowadzone dane
        Button clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Wywołaj metodę clearUserInputData() na adapterze, która wyczyści dane wprowadzone przez użytkownika
                adapter.clearUserInputData();
            }
        });

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Zapisz dane pomiarowe
                saveMeasurementsToFirebase();
            }
        });

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HeartRateMeasurementActivity.this, DashboardActivity.class);
                startActivity(intent);
            }
        });
    }

    private void saveMeasurementsToFirebase() {
        // Pobierz dane pomiarowe z adaptera
        List<Measurement> measurements = adapter.getMeasurements();

        // Wyświetl dane w logach, aby sprawdzić, czy są poprawne
        for (Measurement measurement : measurements) {
            Log.d("TAG", "Tetno: " + measurement.getTetnoValue() + ", Glukoza: " + measurement.getGlukozaValue());
        }

        // Pobierz aktualnie zalogowanego użytkownika
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null) {
            // Pobierz identyfikator użytkownika
            String userId = user.getUid();

            // Stworzenie referencji do konkretnej sekcji pomiarowej w bazie danych
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            DatabaseReference userReference = databaseReference.child("users").child(userId);
            DatabaseReference measurementsReference = userReference.child("measurements");

            // Tworzenie unikalnego identyfikatora dla pomiaru (na podstawie znacznika czasowego)
            String measurementId = generateMeasurementId();

            // Tworzenie nowej sekcji pomiarowej
            DatabaseReference newMeasurementReference = measurementsReference.child(measurementId);

            // Zapisywanie pomiarów jako osobne obiekty w bazie danych
            for (int i = 0; i < measurements.size(); i++) {
                Measurement measurement = measurements.get(i);
                newMeasurementReference.child("tetno" + (i + 1)).setValue(measurement.getTetnoValue());
                newMeasurementReference.child("glukoza" + (i + 1)).setValue(measurement.getGlukozaValue());
            }

            // Wyświetl informację o zapisaniu danych
            Toast.makeText(getApplicationContext(), "Dane pomiarowe zostały zapisane", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateMeasurementId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }
}