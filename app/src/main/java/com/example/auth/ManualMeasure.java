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
        measurements.add(new Measurement("Pomiar 1 - godz. 08:00", "", ""));
        measurements.add(new Measurement("Pomiar 2 - godz. 11:00", "", ""));
        measurements.add(new Measurement("Pomiar 3 - godz. 14:00", "", ""));
        measurements.add(new Measurement("Pomiar 4 - godz. 17:00", "", ""));
        measurements.add(new Measurement("Pomiar 5 - godz. 20:00", "", ""));

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
                if (TextUtils.isEmpty(selectedDate)) {
                    Toast.makeText(getApplicationContext(), "Wybierz datę pomiaru", Toast.LENGTH_SHORT).show();
                } else if (isAllFieldsFilled() && isValid()) {
                    saveMeasurementsToFirebase();
                } else if (!isAllFieldsFilled()){
                    Toast.makeText(getApplicationContext(), "Uzupełnij wszystkie pomiary przed zapisaniem", Toast.LENGTH_SHORT).show();
                }
                else if (!isValid()){
                    Toast.makeText(getApplicationContext(), "Wprowadź wartości dla tętna z zakresu 20-190 oraz dla glukozy z zakresu 20-600", Toast.LENGTH_SHORT).show();
                }
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

    private boolean isAllFieldsFilled() {
        List<Measurement> measurements = adapter.getMeasurements();
        for (Measurement measurement : measurements) {
            if (TextUtils.isEmpty(measurement.getTetnoValue()) || TextUtils.isEmpty(measurement.getGlukozaValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean isValid() {
        List<Measurement> measurements = adapter.getMeasurements();
        for (Measurement measurement : measurements) {
            try {
                int tetno = Integer.parseInt(measurement.getTetnoValue());
                int glukoza = Integer.parseInt(measurement.getGlukozaValue());
                if (tetno < 20 || tetno > 190 || glukoza < 20 || glukoza > 600) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }
    private void saveMeasurementsToFirebase() {
        List<Measurement> measurements = adapter.getMeasurements();
        boolean allFieldsFilled = true;
        boolean isValid = true;

        if (TextUtils.isEmpty(selectedDate)) {
            Toast.makeText(getApplicationContext(), "Wybierz datę pomiaru", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null) {
            String userId = user.getUid();

            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            DatabaseReference userReference = databaseReference.child("users").child(userId);
            DatabaseReference measurementsReference = userReference.child("measurements");
            String measurementId = generateMeasurementId();
            DatabaseReference newMeasurementReference = measurementsReference.child(measurementId);
            DatabaseReference glukozaReference = newMeasurementReference.child("glukoza");
            DatabaseReference tetnoReference = newMeasurementReference.child("tetno");
            newMeasurementReference.child("date").setValue(selectedDate);


            for (int i = 0; i < measurements.size(); i++) {
                Measurement measurement = measurements.get(i);
                String tetnoValue = measurement.getTetnoValue();
                String glukozaValue = measurement.getGlukozaValue();

                // Sprawdzanie, czy pola są wypełnione
                if (TextUtils.isEmpty(tetnoValue) || TextUtils.isEmpty(glukozaValue)) {
                    allFieldsFilled = false;
                    break;
                }

                // Sprawdzanie, czy pola są wypełnione
                if (TextUtils.isEmpty(selectedDate)) {
                    allFieldsFilled = false;
                    break;
                }

                // Sprawdzanie, czy wartości są w odpowiednim zakresie
                try {
                    int tetno = Integer.parseInt(tetnoValue);
                    int glukoza = Integer.parseInt(glukozaValue);

                    if (tetno < 20 || tetno > 190 || glukoza < 20 || glukoza > 600) {
                        isValid = false;
                        break;
                    }
                } catch (NumberFormatException e) {
                    isValid = false;
                    break;
                }


                String measurementTime = getMeasurementTime(i);

                DatabaseReference glukozaValueReference = glukozaReference.child("glukoza" + (i + 1));
                glukozaValueReference.child("date").setValue(selectedDate);
                glukozaValueReference.child("hour").setValue(measurementTime);
                glukozaValueReference.child("value").setValue(glukozaValue);

                DatabaseReference tetnoValueReference = tetnoReference.child("tetno" + (i + 1));
                tetnoValueReference.child("date").setValue(selectedDate);
                tetnoValueReference.child("hour").setValue(measurementTime);
                tetnoValueReference.child("value").setValue(tetnoValue);
            }

            if (!allFieldsFilled) {
                Toast.makeText(getApplicationContext(), "Uzupełnij wszystkie pomiary przed zapisaniem", Toast.LENGTH_SHORT).show();
            } else if (!isValid) {
                Toast.makeText(getApplicationContext(), "Wprowadź wartości dla tętna z zakresu 20-190 oraz dla glukozy z zakresu 20-600", Toast.LENGTH_SHORT).show();
            } else {
                String sessionNumber = generateMeasurementId();
                final String toastMessage = "Dane pomiarowe zostały zapisane\nNumer sesji: " + sessionNumber;
                sessionNumberTextView.setText("Numer zapisanej sesji: " + sessionNumber);

                Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    // Funkcja do pobierania godziny pomiaru na podstawie indeksu
    private String getMeasurementTime(int index) {
        switch (index) {
            case 0:
                return "08:00";
            case 1:
                return "11:00";
            case 2:
                return "14:00";
            case 3:
                return "17:00";
            case 4:
                return "20:00";
            default:
                return "";
        }
    }
    public String getSelectedDate() {
        return selectedDate;
    }

}