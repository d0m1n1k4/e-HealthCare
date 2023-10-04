package com.example.auth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;


import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HeartRateMeasurementActivity extends Activity {

    private TextView headerTextView;
    private FirebaseAuth firebaseAuth;
    private RecyclerView recyclerView;
    private MeasurementAdapter adapter;
    private GoogleSignInClient googleSignInClient;
    private static final int REQUEST_CODE_SIGN_IN = 1;

    // Dodajemy identyfikator klienta OAuth2
    private static final String CLIENT_ID = "281646499375-lkjp0h2ubb2egfr1q2mnmqd0qv9n8bel.apps.googleusercontent.com";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_measurement);

        LinearLayout buttonsLayout = findViewById(R.id.buttonsLayout);


        firebaseAuth = FirebaseAuth.getInstance();

        // Konfiguracja opcji logowania Google SignIn
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/drive.file"))
                .requestIdToken(CLIENT_ID) // Ustawiamy identyfikator klienta OAuth2
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, signInOptions);

        headerTextView = findViewById(R.id.headerTextView);
        headerTextView.setText("Pomiary ręczne");

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

        //Przycisk "WYCZYŚĆ"

        Button clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.clearUserInputData();
            }
        });

        // Przycisk "POBIERZ"

        Button backupButton = findViewById(R.id.backupButton);
        backupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInToGoogleDrive();
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
                Intent intent = new Intent(HeartRateMeasurementActivity.this, DashboardActivity.class);
                startActivity(intent);
            }
        });
    }

    private void signInToGoogleDrive() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                handleDriveSignInSuccess(data);
            } else {
                Log.e("GoogleDriveAuth", "Google Drive sign-in failed.");
                Toast.makeText(getApplicationContext(), "Błąd autoryzacji Google Drive.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleDriveSignInSuccess(Intent data) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            HttpTransport httpTransport;
            try {
                httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    HeartRateMeasurementActivity.this, Collections.singleton("https://www.googleapis.com/auth/drive.file"));
            credential.setSelectedAccount(account.getAccount());

            JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

            Drive driveService = new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName("e-HealthCare")
                    .build();

            saveDataToDrive(driveService);
        } else {
            Log.e("GoogleDriveAuth", "User not signed in to Google Drive.");
            Toast.makeText(getApplicationContext(), "Użytkownik nie jest zalogowany do Google Drive.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDataToDrive(Drive driveService) {
        List<Measurement> measurements = adapter.getMeasurements();
        String jsonData = createJsonData(measurements);

        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName("measurements_" + generateMeasurementId() + ".json");
        fileMetadata.setMimeType("application/json");

        ByteArrayContent content = new ByteArrayContent("application/json", jsonData.getBytes());

        try {
            com.google.api.services.drive.model.File file = driveService.files().create(fileMetadata, content)
                    .setFields("id")
                    .execute();

            Log.d("GoogleDriveSave", "File ID: " + file.getId());
            Toast.makeText(getApplicationContext(), "Plik został zapisany w Dysku Google Drive.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("GoogleDriveSave", "Error saving file to Google Drive: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Błąd podczas zapisywania pliku na Dysku Google Drive.", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateMeasurementId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String createJsonData(List<Measurement> measurements) {
        JSONArray jsonArray = new JSONArray();
        for (Measurement measurement : measurements) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("tetno", measurement.getTetnoValue());
                jsonObject.put("glukoza", measurement.getGlukozaValue());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonArray.toString();
    }

    private void saveMeasurementsToFirebase() {
        List<Measurement> measurements = adapter.getMeasurements();

        for (Measurement measurement : measurements) {
            Log.d("TAG", "Tetno: " + measurement.getTetnoValue() + ", Glukoza: " + measurement.getGlukozaValue());
        }

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

            Toast.makeText(getApplicationContext(), "Dane pomiarowe zostały zapisane", Toast.LENGTH_SHORT).show();
        }
    }
}

