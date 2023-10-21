package com.example.auth;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

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

public class HeartAnalysis extends Activity {

    private Spinner sessionSpinner;
    private String userId;
    private List<String> sessionIds;
    private ArrayAdapter<String> sessionAdapter;
    private LineChart heartRateChart;
    private LineDataSet heartRateDataSet;
    private TextView dateTextView;
    private XAxis xAxis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.heart_analysis);

        sessionSpinner = findViewById(R.id.sessionSpinner);
        heartRateChart = findViewById(R.id.heartRateChart);
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

        loadHeartRateSessionsFromFirebase();

        sessionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedSessionId = sessionIds.get(position);
                searchHeartRateSessionInFirebase(selectedSessionId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        Button heartRateAnalysisBackButton = findViewById(R.id.heartRateAnalysisBackButton);
        heartRateAnalysisBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HeartAnalysis.this, Menu.class);
                startActivity(intent);
            }
        });

        // Inicjalizacja wykresu i zestawu danych
        initHeartRateChart(heartRateChart, "Tętno [bpm]");
    }

    private void loadHeartRateSessionsFromFirebase() {
        if (userId != null) {
            DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
            DatabaseReference heartRateReference = rootReference.child("users").child(userId).child("measurements");

            ValueEventListener valueEventListener = heartRateReference.addValueEventListener(new ValueEventListener() {
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

    private void searchHeartRateSessionInFirebase(String sessionId) {
        if (userId != null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference heartRateReference = database.getReference("users").child(userId).child("measurements");

            DatabaseReference selectedSessionReference = heartRateReference.child(sessionId);

            selectedSessionReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        List<Entry> heartRateEntries = getHeartRateDataFromFirebase(dataSnapshot);

                        String selectedDate = dataSnapshot.child("date").getValue(String.class);

                        dateTextView.setText("Data wykonania pomiaru: " + selectedDate);

                        updateHeartRateChart(heartRateEntries);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }


    private void initHeartRateChart(LineChart chart, String chartTitle) {
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

        // Utworzenie zestawu danych dla wykresu pulsu
        heartRateDataSet = new LineDataSet(new ArrayList<Entry>(), chartTitle);
        heartRateDataSet.setColor(Color.BLACK);
        heartRateDataSet.setCircleColor(Color.BLACK);
        heartRateDataSet.setDrawFilled(false); // Wyłączenie wypełniania obszaru pod linią
        heartRateDataSet.setValueTextSize(10f);

        // Ustawienie etykiet na osi X
        xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // Skok na osi X
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"", "g. 08:00", "g. 11:00", "g. 14:00", "g. 17:00", "g. 20:00"}));
        xAxis.setAxisMinimum(0.5f); // Minimalna wartość na osi X
        xAxis.setAxisMaximum(5.5f);

        LimitLine lowerLimitLine = new LimitLine(60f, "<60 bpm");
        lowerLimitLine.setLineColor(Color.RED);
        lowerLimitLine.setLineWidth(2f);
        lowerLimitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        leftAxis.addLimitLine(lowerLimitLine);

        LimitLine upperLimitLine = new LimitLine(100f, ">100 bpm");
        upperLimitLine.setLineColor(Color.RED);
        upperLimitLine.setLineWidth(2f);
        upperLimitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        leftAxis.addLimitLine(upperLimitLine);

        chart.setExtraBottomOffset(20f);

        chart.getDescription().setText(chartTitle);
        chart.getDescription().setTextSize(15.5f);

        heartRateDataSet.setColor(Color.parseColor("#08CBE2"));
        heartRateDataSet.setLineWidth(2f);

        chart.animateX(500);
        chart.invalidate();
    }

    private List<Entry> getHeartRateDataFromFirebase(DataSnapshot dataSnapshot) {
        // Pobieranie danych z Firebase dla pomiarów pulsu i tworzenie listy Entry, gdzie każdy z elementów zawiera informację o numerze pomiaru i przypisanej do niego wartości pulsu
        List<Entry> entries = new ArrayList<>();

        // Pobieranie danych dla pięciu pomiarów pulsu
        for (int i = 1; i <= 5; i++) {
            DataSnapshot heartRateSnapshot = dataSnapshot.child("tetno").child("tetno" + i);
            String heartRateValue = heartRateSnapshot.child("value").getValue(String.class);
            if (heartRateValue != null) {
                float heartRateFloatValue = Float.parseFloat(heartRateValue);
                entries.add(new Entry((float) i, heartRateFloatValue));
            }
        }

        return entries;
    }

    private void updateHeartRateChart(List<Entry> heartRateEntries) {
        // Usuwanie starych danych z zestawu danych
        heartRateDataSet.clear();

        // Dodawanie nowych danych
        heartRateDataSet.setValues(heartRateEntries);

        // Utworzenie obiektu LineData i dodanie zestawu danych
        LineData heartRateData = new LineData(heartRateDataSet);
        heartRateChart.setData(heartRateData);
        heartRateChart.invalidate();
    }

}
