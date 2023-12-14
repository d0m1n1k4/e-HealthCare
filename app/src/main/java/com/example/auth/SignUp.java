package com.example.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class SignUp extends AppCompatActivity {
    private EditText emailEt, passwordEt1, passwordEt2;
    private Button SignUpButton;
    private TextView SignInTextV;
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up);
        firebaseAuth = FirebaseAuth.getInstance();
        emailEt = findViewById(R.id.email);
        passwordEt1 = findViewById(R.id.password1);
        passwordEt2 = findViewById(R.id.password2);
        SignUpButton = findViewById(R.id.register);
        progressDialog = new ProgressDialog(this);
        SignInTextV = findViewById(R.id.signInTextV);
        SignUpButton.setOnClickListener(v -> Register());
        SignInTextV.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, SignIn.class);
            startActivity(intent);
            finish();
        });
    }

    private void Register() {
        String email = emailEt.getText().toString();
        String password1 = passwordEt1.getText().toString();
        String password2 = passwordEt2.getText().toString();
        boolean hasError = false;

        if (TextUtils.isEmpty(email)) {
            emailEt.setError("Wprowadź adres email");
            hasError = true;
        } else if (!isValidEmail(email)) {
            emailEt.setError("Niepoprawny format adresu email");
            hasError = true;
        }
        if (TextUtils.isEmpty(password1)) {
            passwordEt1.setError("Wprowadź hasło");
            hasError = true;
        } else if (password1.length() < 6) {
            passwordEt1.setError("Hasło powinno zawierać co najmniej 6 znaków");
            hasError = true;
        } else if (!password1.matches(".*[A-Z].*")) {
            passwordEt1.setError("Hasło powinno zawierać co najmniej jedną wielką literę");
            hasError = true;
        } else if (!password1.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            passwordEt1.setError("Hasło powinno zawierać co najmniej jeden znak specjalny");
            hasError = true;
        } else if (!password1.matches(".*\\d.*")) {
            passwordEt1.setError("Hasło powinno zawierać co najmniej jedną cyfrę");
            hasError = true;
        }
        if (TextUtils.isEmpty(password2)) {
            passwordEt2.setError("Potwierdź hasło");
            hasError = true;
        } else if (!password1.equals(password2)) {
            passwordEt2.setError("Wprowadzone hasła nie są jednakowe");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        progressDialog.setMessage("Proszę czekać...");
        progressDialog.show();
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth.createUserWithEmailAndPassword(email, password1).addOnCompleteListener(this, task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(SignUp.this, "Konto zostało pomyślnie utworzone", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(SignUp.this, Menu.class);
                startActivity(intent);
                finish();
            } else {
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    emailEt.setError("Konto o podanym adresie e-mail już istnieje");
                } else {
                    Toast.makeText(SignUp.this, "Rejestracja nie powiodła się", Toast.LENGTH_LONG).show();
                }
            }
        });
    }



    private Boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }
}
