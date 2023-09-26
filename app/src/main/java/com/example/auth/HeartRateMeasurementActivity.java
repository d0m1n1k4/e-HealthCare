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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
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
    private Drive driveService;

    private static final int REQUEST_CODE_SIGN_IN = 1;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_measurement);

        firebaseAuth = FirebaseAuth.getInstance();

        googleSignInClient = GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build());

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

        Button clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.clearUserInputData();
            }
        });

        Button backupButton = findViewById(R.id.backupButton);
        backupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInToGoogleDrive();
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
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        HttpTransport httpTransport;
                        try {
                            httpTransport = com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport();
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }

                        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                                HeartRateMeasurementActivity.this, Collections.singleton(DriveScopes.DRIVE_FILE));
                        credential.setSelectedAccount(googleSignInAccount.getAccount());

                        // Używamy GsonFactory zamiast JacksonFactory
                        JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

                        driveService = new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                                .setApplicationName("Your App Name")
                                .build();

                        saveDataToDrive();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("GoogleDriveAuth", "User not signed in to Google Drive.");
                        Toast.makeText(getApplicationContext(), "Użytkownik nie jest zalogowany do Google Drive.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveDataToDrive() {
        List<Measurement> measurements = adapter.getMeasurements();
        String jsonData = createJsonData(measurements);

        File file = null;

        File fileMetadata = new File();
        fileMetadata.setName("measurements_" + generateMeasurementId() + ".json");
        fileMetadata.setMimeType("application/json");

        com.google.api.client.http.ByteArrayContent content = new com.google.api.client.http.ByteArrayContent("application/json", jsonData.getBytes());

        try {
            file = driveService.files().create(fileMetadata, content)
                    .setFields("id")
                    .execute();
        } catch (IOException e) {
            Log.e("GoogleDriveSave", "Error saving file to Google Drive: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Błąd podczas zapisywania pliku na Dysku Google Drive.", Toast.LENGTH_SHORT).show();
        }

        if (file != null) {
            Log.d("GoogleDriveSave", "File ID: " + file.getId());
            Toast.makeText(getApplicationContext(), "Plik został zapisany w Dysku Google Drive.", Toast.LENGTH_SHORT).show();
        } else {
            Log.e("GoogleDriveSave", "Error saving file to Google Drive.");
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