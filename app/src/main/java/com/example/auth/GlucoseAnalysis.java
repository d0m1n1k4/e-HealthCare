package com.example.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GlucoseAnalysis extends FragmentActivity {

    private Spinner sessionSpinner;
    private String userId;
    private List<String> sessionIds;
    private ArrayAdapter<String> sessionAdapter;
    private LineChart glucoseChart;
    private LineDataSet glucoseDataSet;
    private XAxis xAxis;

    private TextView dateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.glucose_analysis);

        sessionSpinner = findViewById(R.id.sessionSpinner);
        glucoseChart = findViewById(R.id.glucoseChart);
        dateTextView = findViewById(R.id.dateTextView);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            userId = null;
        }

        sessionIds = new ArrayList<>();
        sessionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sessionIds);
        sessionSpinner.setAdapter(sessionAdapter);

        loadMeasurementSessionsFromFirebase();

        sessionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedSessionId = sessionIds.get(position);
                searchSessionInFirebase(selectedSessionId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        Button resultAnalysisBackButton = findViewById(R.id.resultAnalysisBackButton);
        resultAnalysisBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GlucoseAnalysis.this, Menu.class);
                startActivity(intent);
            }
        });

        Button infoButton = findViewById(R.id.infoButton);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlucoseAnalysisInfo infoDialogFragment = new GlucoseAnalysisInfo();
                FragmentManager fragmentManager = getSupportFragmentManager();
                infoDialogFragment.show(fragmentManager, "info_dialog");
            }
        });

        // Inicjalizacja wykresu i zestawu danych
        initChart(glucoseChart, "Poziom glukozy we krwi [mg/gl]");
    }

    private void loadMeasurementSessionsFromFirebase() {
        if (userId != null) {
            DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
            DatabaseReference measurementsReference = rootReference.child("users").child(userId).child("measurements");

            ValueEventListener valueEventListener = measurementsReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    sessionIds.clear();

                    for (DataSnapshot sessionSnapshot : dataSnapshot.getChildren()) {
                        String sessionId = sessionSnapshot.getKey();
                        sessionIds.add(sessionId);
                    }

                    Collections.reverse(sessionIds);
                    sessionAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
    }

    private void searchSessionInFirebase(String sessionId) {
        if (userId != null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference measurementsReference = database.getReference("users").child(userId).child("measurements");

            DatabaseReference selectedSessionReference = measurementsReference.child(sessionId);

            selectedSessionReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        List<Entry> glucoseEntries = getGlucoseDataFromFirebase(dataSnapshot);

                        String selectedDate = dataSnapshot.child("date").getValue(String.class);

                        dateTextView.setText("Data wykonania pomiaru: " + selectedDate);

                        updateGlucoseChart(glucoseEntries);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }

    private void initChart(LineChart chart, String chartTitle) {
        // Ustawianie parametrów osi Y
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setGranularity(1f); // Skok na osi Y
        leftAxis.setSpaceTop(50f);
        chart.getAxisRight().setEnabled(false);

        // Konfigurowanie wykresu
        chart.setDrawGridBackground(false);
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("Brak danych do wyświetlenia.");
        chart.setBackgroundColor(Color.WHITE);

        // Utworzenie zestawu danych dla wykresu glukozy
        glucoseDataSet = new LineDataSet(new ArrayList<Entry>(), chartTitle);
        glucoseDataSet.setColor(Color.BLACK);
        glucoseDataSet.setCircleColor(Color.BLACK);
        glucoseDataSet.setDrawFilled(false); // Wyłączenie wypełniania obszaru pod linią
        glucoseDataSet.setValueTextSize(10f);

        // Ustawienie etykiet na osi X
        xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // Skok na osi X
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"", "g. 08:00", "g. 11:00", "g. 14:00", "g. 17:00", "g. 20:00"}));
        xAxis.setAxisMinimum(-0.02f); // Minimalna wartość na osi X
        xAxis.setAxisMaximum(6f);

        LimitLine lowerLimitLine = new LimitLine(70f, "<70 mg/dl");
        lowerLimitLine.setLineColor(Color.RED);
        lowerLimitLine.setLineWidth(2f);
        lowerLimitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        leftAxis.addLimitLine(lowerLimitLine);

        LimitLine upperLimitLine = new LimitLine(100f, ">100 mg/dl");
        upperLimitLine.setLineColor(Color.RED);
        upperLimitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        upperLimitLine.setLineWidth(        2f);
        leftAxis.addLimitLine(upperLimitLine);

        chart.setExtraBottomOffset(20f);

        chart.getDescription().setText(chartTitle);
        chart.getDescription().setTextSize(15.5f);

        glucoseDataSet.setColor(Color.parseColor("#08CBE2"));
        glucoseDataSet.setLineWidth(2f);

        chart.animateX(500);
        chart.invalidate();
    }

    private List<Entry> getGlucoseDataFromFirebase(DataSnapshot dataSnapshot) {
        // Pobieranie danych z Firebase dla pomiarów glukozy i tworzenie listy Entry, gdzie każdy z elementów zawiera informację o numerze pomiaru i przypisanej do niego wartości poziomu glukozy
        List<Entry> entries = new ArrayList<>();

        // Pobieranie danych dla pięciu pomiarów glukozy
        for (int i = 1; i <= 5; i++) {
            DataSnapshot glukozaSnapshot = dataSnapshot.child("glukoza").child("glukoza" + i);
            String glucoseValue = glukozaSnapshot.child("value").getValue(String.class);
            if (glucoseValue != null) {
                float glucoseFloatValue = Float.parseFloat(glucoseValue);
                entries.add(new Entry((float) i, glucoseFloatValue));
            }
        }

        for (Entry entry : entries) {
            Log.d("GlucoseData", "x: " + entry.getX() + ", y: " + entry.getY());
        }

        return entries;
    }

    private void updateGlucoseChart(List<Entry> glucoseEntries) {
        // Usuwanie starych danych z zestawu danych
        glucoseDataSet.clear();

        // Dodawanie nowych danych
        glucoseDataSet.setValues(glucoseEntries);

        // Utworzenie obiektu LineData i dodanie zestawu danych
        LineData glucoseData = new LineData(glucoseDataSet);
        glucoseChart.setData(glucoseData);
        glucoseChart.invalidate();
    }
}

