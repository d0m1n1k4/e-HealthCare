package com.example.auth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.os.CountDownTimer;

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

public class MeasureManager extends Activity {

    private TextView headerTextView, sessionNumberTextView;
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
        setContentView(R.layout.measure_manager);

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

        sessionNumberTextView = findViewById(R.id.sessionNumberTextView);

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

        // Przycisk "WYCZYŚĆ"
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
                Intent intent = new Intent(MeasureManager.this, Menu.class);
                startActivity(intent);
            }
        });
    }

    private void signInToGoogleDrive() {
        // Inicjowanie procesu logowania do Google Drive.
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SIGN_IN) {
            // Obsługa wyniku aktywności logowania do Google Drive
            if (resultCode == RESULT_OK) {
                handleDriveSignInSuccess(data);
            } else {
                // Wyświetli się log z błędem, jeśli logowanie do Google Drive nie powiodło się.
                String errorMessage = "Błąd autoryzacji Google Drive. Kod błędu: " + resultCode;
                Log.e("GoogleDriveAuth", errorMessage);
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleDriveSignInSuccess(Intent data) {
        // Obsługa sukcesu logowania do Google Drive i inicjacja procesu zapisu danych
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            // Przygotowanie danych uwierzytelniających
            HttpTransport httpTransport;
            try {
                // Obsługa komunikacji z Google Drive API zapewniającej bezpieczne połączenie
                httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            // Tworzenie obiektu służącego do reprezentowania poświadczeń i uprawnień konta Google,
            // które będą używane do autoryzacji dostępu do Google Drive API
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    MeasureManager.this, Collections.singleton("https://www.googleapis.com/auth/drive.file"));
            credential.setSelectedAccount(account.getAccount());

            JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

            // Tworzenie usługi Google Drive i przekazanie jej do metody zapisu danych
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

        // Sprawdź, czy wszystkie pola pomiarów są uzupełnione
        boolean allFieldsFilled = checkAllFieldsFilled(measurements);

        if (allFieldsFilled) {
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName("measurements_" + generateMeasurementId() + ".json");
            fileMetadata.setMimeType("application/json");

            ByteArrayContent content = new ByteArrayContent("application/json", jsonData.getBytes());

            try {
                // Tworzenie pliku w Google Drive i wyświetlenie w logach jego ID po zapisie
                com.google.api.services.drive.model.File file = driveService.files().create(fileMetadata, content)
                        .setFields("id")
                        .execute();

                Log.d("GoogleDriveSave", "File ID: " + file.getId());
                Toast.makeText(getApplicationContext(), "Plik został zapisany w Dysku Google Drive.", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e("GoogleDriveSave", "Error saving file to Google Drive: " + e.getMessage());
                Toast.makeText(getApplicationContext(), "Błąd podczas zapisywania pliku na Dysku Google Drive.", Toast.LENGTH_SHORT).show();
            }

            String sessionNumber = generateMeasurementId();
            sessionNumberTextView.setText("Numer zapisanej sesji: " + sessionNumber);
        } else {
            // Wyświetl komunikat o błędzie, jeśli nie wszystkie pola pomiarów są uzupełnione
            Toast.makeText(getApplicationContext(), "Uzupełnij wszystkie pola pomiarów przed zapisaniem.", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateMeasurementId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String createJsonData(List<Measurement> measurements) {
        // Tworzenie danych pomiarowych w formacie JSON na podstawie listy pomiarów
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

    private boolean checkAllFieldsFilled(List<Measurement> measurements) {
        for (Measurement measurement : measurements) {
            String tetnoValue = measurement.getTetnoValue();
            String glukozaValue = measurement.getGlukozaValue();
            if (TextUtils.isEmpty(tetnoValue) || TextUtils.isEmpty(glukozaValue)) {
                return false;
            }
        }
        return true;
    }

    private void saveMeasurementsToFirebase() {
        List<Measurement> measurements = adapter.getMeasurements();
        boolean isValid = true;

        for (Measurement measurement : measurements) {
            String tetnoValue = measurement.getTetnoValue();
            String glukozaValue = measurement.getGlukozaValue();

            // Sprawdź, czy wartości wprowadzonego tętna i glukozy są liczbami całkowitymi z zakresu 20-450
            try {
                int tetno = Integer.parseInt(tetnoValue);
                int glukoza = Integer.parseInt(glukozaValue);

                if (tetno < 20 || tetno > 450 || glukoza < 20 || glukoza > 450) {
                    isValid = false;
                    break;
                }
            } catch (NumberFormatException e) {
                isValid = false;
                break;
            }
        }

        if (isValid) {
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

                // Wyświetl komunikat "Dane pomiarowe zostały zapisane - numer sesji: yyyyMMdd_HHmmss"
                String sessionNumber = generateMeasurementId();
                final String toastMessage = "Dane pomiarowe zostały zapisane\nNumer sesji: " + sessionNumber;
                sessionNumberTextView.setText("Numer zapisanej sesji: " + sessionNumber);

                Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();

                // Timer do ukrycia komunikatu po określonym czasie
                new CountDownTimer(2000, 2000) {
                    public void onTick(long millisUntilFinished) {
                        Toast toast = Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG);
                        toast.show();
                    }

                    public void onFinish() {
                        // Komunikat zostanie automatycznie zamknięty po zakończeniu timera
                    }
                }.start();
            }
        } else {
            // Wyświetl komunikat o błędzie
            Toast.makeText(getApplicationContext(), "Przed zapisaniem sesji uzupełnij wszystkie pomiary liczbami całkowitymi z zakresu 20-450", Toast.LENGTH_SHORT).show();
        }
    }
}