package com.example.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignIn extends AppCompatActivity {
    private EditText emailEt, passwordEt;
    private Button SignInButton;
    private TextView SignUpTextV;
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in);

        firebaseAuth = FirebaseAuth.getInstance();
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
                Intent intent = new Intent(SignIn.this, SignUp.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void Login() {
        String email = emailEt.getText().toString();
        String password = passwordEt.getText().toString();
        if (TextUtils.isEmpty(email)) {
            emailEt.setError("Wprowadź adres email");
            return;
        } else if (TextUtils.isEmpty(password)) {
            passwordEt.setError("Wprowadź hasło");
            return;
        }
        progressDialog.setMessage("Proszę czekać...");
        progressDialog.show();
        progressDialog.setCanceledOnTouchOutside(false);
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(SignIn.this, "Logowanie przebiegło pomyślnie", Toast.LENGTH_LONG).show();
                    // Dodaj informacje o zalogowanym użytkowniku do Firebase Realtime Database
                    addUserToDatabase(firebaseAuth.getCurrentUser().getUid());
                    Intent intent = new Intent(SignIn.this, Menu.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(SignIn.this, "Logowanie nie powiodło się", Toast.LENGTH_LONG).show();
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