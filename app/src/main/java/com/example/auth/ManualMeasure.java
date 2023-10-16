package com.example.auth;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.DatePicker;


import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManualMeasure extends Activity {

    private TextView headerTextView, sessionNumberTextView, selectedDateTextView;
    private FirebaseAuth firebaseAuth;
    private RecyclerView recyclerView;
    private MeasurementAdapter adapter;

    // Dodatkowe pole do przechowywania daty
    private String selectedDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manual_measure);

        LinearLayout buttonsLayout = findViewById(R.id.buttonsLayout);

        firebaseAuth = FirebaseAuth.getInstance();

        headerTextView = findViewById(R.id.headerTextView);
        headerTextView.setText("Pomiary ręczne");

        sessionNumberTextView = findViewById(R.id.sessionNumberTextView);
        selectedDateTextView = findViewById(R.id.selectedDateTextView);

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

        Button clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.clearUserInputData();
            }
        });

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMeasurementsToFirebase();
            }
        });

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Obsługa przycisku "DATA POMIARU"
        Button selectDateButton = findViewById(R.id.selectDateButton);
        selectDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });
    }

    private void showDatePickerDialog() {
        // Pobieranie aktualnej daty
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                // Aktualizacja wybranej daty i wyświetlenie na ekranie
                selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
                selectedDateTextView.setText("Data pomiaru: " + selectedDate);
            }
        }, year, month, day);

        datePickerDialog.show();
    }

    private String generateMeasurementId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALY);
        return sdf.format(new Date());
    }

    private void saveMeasurementsToFirebase() {
        List<Measurement> measurements = adapter.getMeasurements();
        boolean allFieldsFilled = true;
        boolean isValid = true;

        for (Measurement measurement : measurements) {
            String tetnoValue = measurement.getTetnoValue();
            String glukozaValue = measurement.getGlukozaValue();

            if (TextUtils.isEmpty(tetnoValue) || TextUtils.isEmpty(glukozaValue)) {
                allFieldsFilled = false;
                break;
            }
        }

        if (!allFieldsFilled) {
            Toast.makeText(getApplicationContext(), "Uzupełnij wszystkie pomiary przed zapisaniem", Toast.LENGTH_SHORT).show();
        } else {
            for (Measurement measurement : measurements) {
                String tetnoValue = measurement.getTetnoValue();
                String glukozaValue = measurement.getGlukozaValue();

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

            if (!isValid) {
                Toast.makeText(getApplicationContext(), "Wprowadź wartości z zakresu 20-450", Toast.LENGTH_SHORT).show();
            } else {
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

                    if (!TextUtils.isEmpty(selectedDate)) {
                        // Dodawanie daty do bazy danych
                        newMeasurementReference.child("date").setValue(selectedDate);
                    }

                    String sessionNumber = generateMeasurementId();
                    final String toastMessage = "Dane pomiarowe zostały zapisane\nNumer sesji: " + sessionNumber;
                    sessionNumberTextView.setText("Numer zapisanej sesji: " + sessionNumber);

                    Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}

