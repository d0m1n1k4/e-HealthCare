package com.example.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
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

public class DownloadResults extends Activity {

    private Spinner sessionSpinner;
    private String userId;
    private List<String> sessionIds;
    private ArrayAdapter<String> sessionAdapter;
    private Button driveButton;
    private GoogleSignInClient googleSignInClient;
    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final String ANDROID_CLIENT_ID = "281646499375-lkjp0h2ubb2egfr1q2mnmqd0qv9n8bel.apps.googleusercontent.com";
    private static final String WEB_CLIENT_ID = "281646499375-q6jbaucmkbdln8bqrfuqrc0idadfi1de.apps.googleusercontent.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_results);

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

    private void signInToGoogleDrive() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/drive.file"))
                .requestIdToken(WEB_CLIENT_ID) // Użyj identyfikatora klienta internetowego
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

    private void handleDriveSignInSuccess(GoogleSignInAccount account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                DownloadResults.this, Collections.singleton("https://www.googleapis.com/auth/drive.file"));
        credential.setSelectedAccount(account.getAccount());

        HttpTransport httpTransport;
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

        Drive driveService = new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName("e-HealthCare")
                .build();

        Toast.makeText(getApplicationContext(), "Zalogowano do Google Drive.", Toast.LENGTH_SHORT).show();
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
