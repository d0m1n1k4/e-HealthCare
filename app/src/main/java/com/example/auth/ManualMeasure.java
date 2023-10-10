package com.example.auth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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

public class ManualMeasure extends Activity {

    private TextView headerTextView, sessionNumberTextView;
    private FirebaseAuth firebaseAuth;
    private RecyclerView recyclerView;
    private MeasurementAdapter adapter;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manual_measure);

        LinearLayout buttonsLayout = findViewById(R.id.buttonsLayout);

        firebaseAuth = FirebaseAuth.getInstance();

        headerTextView = findViewById(R.id.headerTextView);
        headerTextView.setText("Pomiary ręczne");

        sessionNumberTextView = findViewById(R.id.sessionNumberTextView);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Measurement> measurements = new ArrayList<>();
        measurements.add(new Measurement("Pomiar 1", "", ""));
        measurements.add(new Measurement("Pomiar 2", "", ""));
        measurements.add(new Measurement("Pomiar 3", "", ""));
        measurements.add(new Measurement("Pomiar 4", "", ""));
        measurements.add(new Measurement("Pomiar 5", "", ""));

        adapter = new MeasurementAdapter(measurements);
        recyclerView.setAdapter(adapter);

        // Przycisk "WYCZYŚĆ"
        Button clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.clearUserInputData();
            }
        });


        // Przycisk "ZAPISZ"
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMeasurementsToFirebase();
            }
        });

        // Przycisk "POWRÓT"
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ManualMeasure.this, Menu.class);
                startActivity(intent);
            }
        });
    }


    private String generateMeasurementId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void saveMeasurementsToFirebase() {
        List<Measurement> measurements = adapter.getMeasurements();
        boolean isValid = true;

        for (Measurement measurement : measurements) {
            String tetnoValue = measurement.getTetnoValue();
            String glukozaValue = measurement.getGlukozaValue();

            // Sprawdź, czy wartości wprowadzonego tętna i glukozy są liczbami całkowitymi z zakresu 20-450
            try {
                int tetno = Integer.parseInt(tetnoValue);
                int glukoza = Integer.parseInt(glukozaValue);

                if (tetno < 20 || tetno > 450 || glukoza < 20 || glukoza > 450) {
                    isValid = false;
                    break;
                }
            } catch (NumberFormatException e) {
                isValid = false;
                break;
            }
        }

        if (isValid) {
            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user != null) {
                String userId = user.getUid();
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                DatabaseReference userReference = databaseReference.child("users").child(userId);
                DatabaseReference measurementsReference = userReference.child("measurements");
                String measurementId = generateMeasurementId();
                DatabaseReference newMeasurementReference = measurementsReference.child(measurementId);

                for (int i = 0; i < measurements.size(); i++) {
                    Measurement measurement = measurements.get(i);
                    newMeasurementReference.child("tetno" + (i + 1)).setValue(measurement.getTetnoValue());
                    newMeasurementReference.child("glukoza" + (i + 1)).setValue(measurement.getGlukozaValue());
                }

                // Wyświetl komunikat "Dane pomiarowe zostały zapisane - numer sesji: yyyyMMdd_HHmmss"
                String sessionNumber = generateMeasurementId();
                final String toastMessage = "Dane pomiarowe zostały zapisane\nNumer sesji: " + sessionNumber;
                sessionNumberTextView.setText("Numer zapisanej sesji: " + sessionNumber);

                Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();

                // Timer do ukrycia komunikatu po określonym czasie
                new CountDownTimer(2000, 2000) {
                    public void onTick(long millisUntilFinished) {
                        Toast toast = Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG);
                        toast.show();
                    }

                    public void onFinish() {
                        // Komunikat zostanie automatycznie zamknięty po zakończeniu timera
                    }
                }.start();
            }
        } else {
            // Wyświetl komunikat o błędzie
            Toast.makeText(getApplicationContext(), "Przed zapisaniem sesji uzupełnij wszystkie pomiary liczbami całkowitymi z zakresu 20-450", Toast.LENGTH_SHORT).show();
        }
    }
}