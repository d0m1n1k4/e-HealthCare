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

public class SignUp extends AppCompatActivity {
    private EditText emailEt, passwordEt1, passwordEt2;
    private Button SignUpButton;
    private TextView SignInTextV;
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);
        firebaseAuth = FirebaseAuth.getInstance();
        emailEt = findViewById(R.id.email);
        passwordEt1 = findViewById(R.id.password1);
        passwordEt2 = findViewById(R.id.password2);
        SignUpButton = findViewById(R.id.register);
        progressDialog = new ProgressDialog(this);
        SignInTextV = findViewById(R.id.signInTextV);
        SignUpButton.setOnClickListener(v -> Register());
        SignInTextV.setOnClickListener(v -> {
            Intent intent=new Intent(SignUp.this, SignIn.class);
            startActivity(intent);
            finish();
        });
    }

    private void Register() {
        String email = emailEt.getText().toString();
        String password1 = passwordEt1.getText().toString();
        String password2 = passwordEt2.getText().toString();
        if (TextUtils.isEmpty(email)) {
            emailEt.setError("Enter your email");
            return;
        } else if (TextUtils.isEmpty(password1)) {
            passwordEt1.setError("Enter your password");
            return;
        } else if (TextUtils.isEmpty(password2)) {
            passwordEt2.setError("Confirm your password");
            return;
        } else if (!password1.equals(password2)) {
            passwordEt2.setError("Different password");
            return;
        } else if (password1.length() < 4) {
            passwordEt1.setError("Length should be >4 characters");
            return;
        } else if (!isValidEmail(email)) {
            emailEt.setError("Invalid email");
            return;
        }
        progressDialog.setMessage("Please wait...");
        progressDialog.show();
        progressDialog.setCanceledOnTouchOutside(false);
        firebaseAuth.createUserWithEmailAndPassword(email,password1).addOnCompleteListener(this, task -> {
            if(task.isSuccessful()){
                Toast.makeText(SignUp.this, "Successfully registered", Toast.LENGTH_LONG).show();
                Intent intent=new Intent(SignUp.this, Menu.class);
                startActivity(intent);
                finish();
            }
            else{
                Toast.makeText(SignUp.this, "Sign up fail!", Toast.LENGTH_LONG).show();
            }
            progressDialog.dismiss();

        });
    }
    private Boolean isValidEmail(CharSequence target){
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

}