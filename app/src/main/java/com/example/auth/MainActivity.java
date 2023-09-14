package com.example.auth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase; // Dodaj import Firebase Realtime Database

public class MainActivity extends AppCompatActivity {
    private EditText emailEt, passwordEt;
    private Button SignInButton;
    private TextView SignUpTextV;
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference; // Dodaj referencję do bazy danych Firebase Realtime Database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicjalizacja Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // Inicjalizacja Firebase Realtime Database
        databaseReference = FirebaseDatabase.getInstance().getReference();

        emailEt = findViewById(R.id.email);
        passwordEt = findViewById(R.id.password);
        SignInButton = findViewById(R.id.login);
        progressDialog = new ProgressDialog(this);
        SignUpTextV = findViewById(R.id.signUpTextV);
        SignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Login();
            }
        });
        SignUpTextV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void Login() {
        String email = emailEt.getText().toString();
        String password = passwordEt.getText().toString();
        if (TextUtils.isEmpty(email)) {
            emailEt.setError("Enter your email");
            return;
        } else if (TextUtils.isEmpty(password)) {
            passwordEt.setError("Enter your password");
            return;
        }
        progressDialog.setMessage("Please wait...");
        progressDialog.show();
        progressDialog.setCanceledOnTouchOutside(false);
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Successfully registered", Toast.LENGTH_LONG).show();
                    // Dodaj informacje o zalogowanym użytkowniku do Firebase Realtime Database
                    addUserToDatabase(firebaseAuth.getCurrentUser().getUid());

                    Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "Sign In fail!", Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
            }
        });
    }

    // Metoda dodająca informacje o zalogowanym użytkowniku do Firebase Realtime Database
    private void addUserToDatabase(String userId) {
        // Tworzymy referencję do konkretnego użytkownika w bazie danych
        DatabaseReference userReference = databaseReference.child("users").child(userId);

    }
}