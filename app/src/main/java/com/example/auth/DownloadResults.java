package com.example.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DownloadResults extends Activity {

    public static final String SPINNER_MOCK_ITEM_1 = "-- brak sesji --";
    private Spinner sessionSpinner;
    private String userId;
    private List<String> sessionIds = new ArrayList<>();
    private List<Measurement> measurements = new ArrayList<>();

    private ArrayAdapter<String> sessionAdapter;
    private MeasurementAdapter measurementAdapter;

    private Button driveButton;
    private Button backButton;

    private boolean isFileSaved = false;

    private GoogleSignInClient googleSignInClient;
    private static final int REQUEST_CODE_SIGN_IN = 1;

    private static final String WEB_CLIENT_ID = "281646499375-q6jbaucmkbdln8bqrfuqrc0idadfi1de.apps.googleusercontent.com";

    private Measurements selectedMeasurements;
    private String selectedSpinnerItemValue;

    private Executor executor = Executors.newSingleThreadExecutor();
    private Handler handler;

    private static final String TAG = "DownloadResults";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_results);

        handler = new Handler(Looper.getMainLooper());

        sessionSpinner = findViewById(R.id.downloadSessionSpinner);
        driveButton = findViewById(R.id.driveButton);
        backButton = findViewById(R.id.downloadResultsBackButton);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            userId = null;
        }

        sessionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sessionIds);
        sessionSpinner.setAdapter(sessionAdapter);

        sessionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String sessionId = sessionIds.get(position);
                selectedSpinnerItemValue = sessionId;

                if (sessionId.equals(SPINNER_MOCK_ITEM_1)) {
                    return;
                }

                asyncGetMeasurementsForSessionId(sessionId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        asyncLoadMeasurementsToSessionAdapter();

        measurementAdapter = new MeasurementAdapter(measurements);

        driveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SPINNER_MOCK_ITEM_1.equals(selectedSpinnerItemValue)) {
                    Toast.makeText(DownloadResults.this, "Wybierz najpierw numer ID sesji do pobrania", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isFileSaved) {
                    signInToGoogleDrive();
                } else {
                    Toast.makeText(DownloadResults.this, "Plik z wynikami wybranej sesji został już zapisany w Google Drive.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DownloadResults.this, Menu.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        handler = null;
    }

    private void signInToGoogleDrive() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/drive.file"))
                .requestIdToken(WEB_CLIENT_ID)
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, signInOptions);

        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleDriveSignInResult(task);
        }
    }

    private void handleDriveSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            if (account != null) {
                onGoogleDriveSignInSuccess(account);
            } else {
                Log.e("GoogleDriveAuth", "GoogleSignInAccount is null.");
                Toast.makeText(getApplicationContext(), "Błąd autoryzacji Google Drive.", Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            Log.e("GoogleDriveAuth", "Error signing in to Google Drive: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Błąd autoryzacji Google Drive.", Toast.LENGTH_SHORT).show();
        }
    }

    private Drive getDriveService(GoogleAccountCredential credential) {
        HttpTransport httpTransport = new NetHttpTransport();

        return new Drive.Builder(httpTransport, new GsonFactory(), credential)
                .build();
    }

    private void onGoogleDriveSignInSuccess(GoogleSignInAccount account) {
        Toast.makeText(getApplicationContext(), "Zalogowano do Google Drive.", Toast.LENGTH_SHORT).show();

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                this,
                Collections.singleton("https://www.googleapis.com/auth/drive.file")
        );

        credential.setSelectedAccount(account.getAccount());

        Drive driveService = getDriveService(credential);

        // Sprawdzenie, czy dane są gotowe do pobrania
        if (selectedMeasurements == null) {
            Toast.makeText(this, "Dane pomiarowe niedostępne", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tworzenie pliku JSON
        String jsonData = new GsonBuilder().setPrettyPrinting().create().toJson(selectedMeasurements);
        String jsonFileName = selectedMeasurements.sessionId + ".json";

        executor.execute(()
                -> {
            try {
                File file = createJsonFile(driveService, jsonFileName, jsonData);

                handler.post(() -> {
                    isFileSaved = true;
                    Toast.makeText(DownloadResults.this, "Plik JSON został zapisany w Google Drive: " + jsonFileName, Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private File createJsonFile(Drive driveService, String fileName, String fileContent) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setMimeType("application/json");

        ByteArrayContent mediaContent = ByteArrayContent.fromString("application/json", fileContent);

        File file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        return file;
    }

    private String getMeasurementTime(int index) {
        // Implementacja pobierania czasu pomiaru na podstawie indeksu
        return "CZAS POMIARU";
    }

    private void asyncLoadMeasurementsToSessionAdapter() {
        if (userId != null) {
            DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
            DatabaseReference heartRateReference = rootReference.child("users").child(userId).child("measurements");
            heartRateReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    sessionIds.clear();

                    for (DataSnapshot sessionSnapshot : dataSnapshot.getChildren()) {
                        String sessionId = sessionSnapshot.getKey();
                        sessionIds.add(sessionId);
                    }

                    if (sessionIds.size() == 0) {
                        sessionIds.add(SPINNER_MOCK_ITEM_1);
                    } else {
                        Collections.reverse(sessionIds);
                    }

                    sessionAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Obsługa błędów
                }
            });
        }
    }

    private void asyncGetMeasurementsForSessionId(String sessionId) {
        if (userId != null) {
            // Usuwanie starych sesji pomiarowych przed pobraniem następnej
            selectedMeasurements = null;

            DatabaseReference sessionsReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("measurements");
            DatabaseReference selectedSessionReference = sessionsReference.child(sessionId);

            selectedSessionReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Measurements measurements = dataSnapshot.getValue(Measurements.class);
                    measurements.setSessionId(sessionId);

                    // Przechowanie nowej sesji pomiarowej
                    selectedMeasurements = measurements;
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Obsługa błędów
                }
            });
        }
    }
}