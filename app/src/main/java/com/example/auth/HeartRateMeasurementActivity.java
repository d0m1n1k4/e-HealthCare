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
import com.google.android.gms.drive.CreateFileActivityOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HeartRateMeasurementActivity extends Activity {

    private TextView headerTextView;
    private FirebaseAuth firebaseAuth;
    private RecyclerView recyclerView;
    private MeasurementAdapter adapter;
    private GoogleSignInClient googleSignInClient;
    private DriveClient driveClient;
    private DriveFolder appFolder;

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
        // Rozpocznij proces autoryzacji Google Drive po kliknięciu przycisku "POBIERZ"
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Autoryzacja w Google Drive zakończona sukcesem
                handleDriveSignInSuccess();
            } else {
                Log.e("GoogleDriveAuth", "Google Drive sign-in failed.");
                Toast.makeText(getApplicationContext(), "Błąd autoryzacji Google Drive.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleDriveSignInSuccess() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            String jsonFileName = "measurements_" + generateMeasurementId() + ".json";
            String jsonData = createJsonData(adapter.getMeasurements());

            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(jsonFileName)
                    .setMimeType("application/json")
                    .setStarred(false)
                    .build();

            Drive.getDriveResourceClient(this, account) // Użyj konta autoryzacji
                    .createContents()
                    .addOnSuccessListener(new OnSuccessListener<DriveContents>() {
                        @Override
                        public void onSuccess(DriveContents driveContents) {
                            OutputStream outputStream = driveContents.getOutputStream();
                            try {
                                outputStream.write(jsonData.getBytes());
                                outputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Drive.getDriveResourceClient(HeartRateMeasurementActivity.this, account)
                                    .getRootFolder()
                                    .addOnSuccessListener(new OnSuccessListener<DriveFolder>() {
                                        @Override
                                        public void onSuccess(DriveFolder driveFolder) {
                                            appFolder = driveFolder;

                                            Drive.getDriveResourceClient(HeartRateMeasurementActivity.this, account)
                                                    .createFile(appFolder, changeSet, driveContents)
                                                    .addOnSuccessListener(new OnSuccessListener<DriveFile>() {
                                                        @Override
                                                        public void onSuccess(DriveFile driveFile) {
                                                            Log.d("GoogleDriveSave", "File saved to Google Drive.");
                                                            Toast.makeText(getApplicationContext(), "Plik JSON został zapisany w Google Drive.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.e("GoogleDriveSave", "Error saving file to Google Drive: " + e.getMessage());
                                                            Toast.makeText(getApplicationContext(), "Błąd przy zapisywaniu pliku w Google Drive.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e("GoogleDriveFolder", "Error accessing root folder in Google Drive.");
                                            Toast.makeText(getApplicationContext(), "Błąd dostępu do folderu głównego w Google Drive.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("GoogleDriveSave", "Error creating file contents: " + e.getMessage());
                            Toast.makeText(getApplicationContext(), "Błąd przy tworzeniu zawartości pliku.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Log.e("GoogleDriveAuth", "User not signed in to Google Drive.");
            Toast.makeText(getApplicationContext(), "Użytkownik nie jest zalogowany do Google Drive.", Toast.LENGTH_SHORT).show();
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

}
