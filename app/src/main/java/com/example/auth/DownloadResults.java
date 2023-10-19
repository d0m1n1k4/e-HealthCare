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
import com.nimbusds.jose.shaded.json.JSONObject;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DownloadResults extends Activity {

    private Spinner sessionSpinner;
    private String userId;
    private List<String> sessionIds;
    private ArrayAdapter<String> sessionAdapter;
    private List<Measurement> measurements = new ArrayList<>();

    private MeasurementAdapter adapter;

    private Button driveButton;

    private boolean isFileSaved = false;
    private GoogleSignInClient googleSignInClient;
    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final String ANDROID_CLIENT_ID = "281646499375-lkjp0h2ubb2egfr1q2mnmqd0qv9n8bel.apps.googleusercontent.com";
    private static final String WEB_CLIENT_ID = "281646499375-q6jbaucmkbdln8bqrfuqrc0idadfi1de.apps.googleusercontent.com";

    private String selectedDate = "";

    private String selectedSessionId = "";
    private Executor executor = Executors.newSingleThreadExecutor();
    private Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_results);

        handler = new Handler(Looper.getMainLooper());

        sessionSpinner = findViewById(R.id.downloadSessionSpinner);
        driveButton = findViewById(R.id.driveButton);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            userId = null;
        }

        sessionIds = new ArrayList<>();
        sessionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sessionIds);
        sessionSpinner.setAdapter(sessionAdapter);

        // Inicjalizacja adaptera
        adapter = new MeasurementAdapter(measurements);

        loadHeartRateSessionsFromFirebase();

        sessionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedSessionId = sessionIds.get(position);
                String selectedSessionId = sessionIds.get(position);
                searchHeartRateSessionInFirebase(selectedSessionId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        driveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isFileSaved) { // Sprawdź, czy plik nie został już zapisany
                    signInToGoogleDrive();
                } else {
                    Toast.makeText(DownloadResults.this, "Plik z wynikami sesji " + selectedSessionId + " został już zapisany w Google Drive.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        Button backButton = findViewById(R.id.downloadResultsBackButton);
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
                handleDriveSignInSuccess(account);
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

    private void handleDriveSignInSuccess(GoogleSignInAccount account) {
        Toast.makeText(getApplicationContext(), "Zalogowano do Google Drive.", Toast.LENGTH_SHORT).show();

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                this, Collections.singleton("https://www.googleapis.com/auth/drive.file"));
        credential.setSelectedAccount(account.getAccount());

        Drive driveService = getDriveService(credential);

        // Tworzenie pliku JSON
        String jsonData = createJsonData();
        String jsonFileName = selectedSessionId + ".json";

        executor.execute(() -> {
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

    private String createJsonData() {
        JSONObject sessionData = new JSONObject();
        sessionData.put("id", selectedSessionId);

        JSONArray glukozaArray = new JSONArray();
        JSONArray tetnoArray = new JSONArray();
        List<Measurement> measurements = adapter.getMeasurements();

        for (int i = 0; i < measurements.size(); i++) {
            Measurement measurement = measurements.get(i);
            String tetnoValue = measurement.getTetnoValue();
            String glukozaValue = measurement.getGlukozaValue();
            String measurementTime = getMeasurementTime(i);

            if (!tetnoValue.isEmpty() && !glukozaValue.isEmpty()) {
                JSONObject glukozaItem = new JSONObject();
                glukozaItem.put("date", selectedDate);
                glukozaItem.put("time", measurementTime);
                glukozaItem.put("value", Integer.parseInt(glukozaValue));

                JSONObject tetnoItem = new JSONObject();
                tetnoItem.put("date", selectedDate);
                tetnoItem.put("time", measurementTime);
                tetnoItem.put("value", Integer.parseInt(tetnoValue));

                glukozaArray.put(glukozaItem);
                tetnoArray.put(tetnoItem);
            }
        }

        sessionData.put("glukoza", glukozaArray);
        sessionData.put("tetno", tetnoArray);

        return sessionData.toString();
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

    private String generateMeasurementId() {
        return selectedSessionId;
    }

    private String getMeasurementTime(int index) {
        // Implementacja pobierania czasu pomiaru na podstawie indeksu
        return "CZAS POMIARU";
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
                    // Obsługa błędów
                }
            });
        }
    }

    private void searchHeartRateSessionInFirebase(String sessionId) {
        if (userId != null) {
            DatabaseReference sessionsReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("sessions");

            DatabaseReference selectedSessionReference = sessionsReference.child(sessionId);

            selectedSessionReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Obsługa błędów
                }
            });
        }
    }
}