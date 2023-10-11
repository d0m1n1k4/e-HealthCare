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

import org.json.JSONException;
import org.json.JSONObject;

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

    private DatabaseReference sessionsReference;
    private ArrayAdapter<String> sessionAdapter;
    private Button driveButton;
    private GoogleSignInClient googleSignInClient;

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final String ANDROID_CLIENT_ID = "281646499375-lkjp0h2ubb2egfr1q2mnmqd0qv9n8bel.apps.googleusercontent.com";
    private static final String WEB_CLIENT_ID = "281646499375-q6jbaucmkbdln8bqrfuqrc0idadfi1de.apps.googleusercontent.com";

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

        // Pobieranie referencji do Firebase dla użytkownika
        if (userId != null) {
            DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
            sessionsReference = rootReference.child("users").child(userId).child("sessions");
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

        driveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInToGoogleDrive();
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
                .requestIdToken(WEB_CLIENT_ID) // Użycie identyfikatora klienta internetowego
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


    private String TAG = "DownloadResults";

    private void handleDriveSignInSuccess(GoogleSignInAccount account) {
        Toast.makeText(getApplicationContext(), "Zalogowano do Google Drive.", Toast.LENGTH_SHORT).show();

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                this, Collections.singleton("https://www.googleapis.com/auth/drive.file"));
        credential.setSelectedAccount(account.getAccount());

        Drive driveService = getDriveService(credential);

        // Pobieranie wybranej sesji z Firebase
        DatabaseReference selectedSessionReference = sessionsReference.child(sessionSpinner.getSelectedItem().toString());

        selectedSessionReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Tworzenie pliku JSON z danymi sesji pomiarowej
                JSONObject sessionData = new JSONObject();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String key = child.getKey();
                    String value = child.getValue(String.class);
                    try {
                        sessionData.put(key, value);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                // Zapisywanie pliku JSON do Google Drive
                String sessionName = sessionSpinner.getSelectedItem().toString();
                String fileName = sessionName + ".json";
                createJsonFileInDrive(driveService, fileName, sessionData.toString());

                handler.post(() -> {
                    Toast.makeText(DownloadResults.this, "Plik JSON utworzony w Google Drive.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void createJsonFileInDrive(Drive driveService, String fileName, String jsonContent) {
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setMimeType("application/json");

        ByteArrayContent mediaContent = ByteArrayContent.fromString("application/json", jsonContent);

        executor.execute(() -> {
            try {
                File file = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute();

                handler.post(() -> {
                    Toast.makeText(DownloadResults.this, "Plik JSON utworzony w Google Drive. ID pliku: " + file.getId(), Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    private void searchHeartRateSessionInFirebase(String sessionId) {
        if (userId != null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference sessionsReference = database.getReference("users").child(userId).child("sessions");

            DatabaseReference selectedSessionReference = sessionsReference.child(sessionId);

            selectedSessionReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
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
}