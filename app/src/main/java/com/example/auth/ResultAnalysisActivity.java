package com.example.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultAnalysisActivity extends Activity {

    private Spinner sessionSpinner;
    private String userId;
    private List<String> sessionIds;
    private ArrayAdapter<String> sessionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_analysis);

        sessionSpinner = findViewById(R.id.sessionSpinner);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            userId = null;
        }

        sessionIds = new ArrayList<>();
        sessionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sessionIds);
        sessionSpinner.setAdapter(sessionAdapter);

        sessionIds.add("Szukaj");

        loadMeasurementSessionsFromFirebase();

        sessionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedSessionId = sessionIds.get(position);
                if (!selectedSessionId.equals("Szukaj")) {
                    searchSessionInFirebase(selectedSessionId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        Button resultAnalysisBackButton = findViewById(R.id.resultAnalysisBackButton);
        resultAnalysisBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultAnalysisActivity.this, DashboardActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadMeasurementSessionsFromFirebase() {
        if (userId != null) {
            DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
            DatabaseReference measurementsReference = rootReference.child("users").child(userId).child("measurements");

            measurementsReference.addValueEventListener(new ValueEventListener() {
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
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }

    private void searchSessionInFirebase(String sessionId) {
        if (userId != null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference measurementsReference = database.getReference("users").child(userId).child("measurements");

            Query query = measurementsReference.orderByChild("sessionId").equalTo(sessionId);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot sessionSnapshot : dataSnapshot.getChildren()) {
                            String tetno1 = sessionSnapshot.child("tetno1").getValue(String.class);
                            String glukoza1 = sessionSnapshot.child("glukoza1").getValue(String.class);

                        }
                    }                 }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }
}
