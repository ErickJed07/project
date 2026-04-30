package com.example.project;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class C_SignInActivity extends AppCompatActivity {

    private EditText emailInput, usernameInput, passwordInput, confirmPasswordInput;
    private TextView emailLabel, usernameLabel, passwordLabel, confirmPasswordLabel;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.c_signin); // Ensure this XML has the ImageView removed or ignored

        mAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.et_email_input);
        usernameInput = findViewById(R.id.et_username_input);
        passwordInput = findViewById(R.id.et_password_input);
        confirmPasswordInput = findViewById(R.id.et_confirm_password_input);

        emailLabel = findViewById(R.id.tv_email_label);
        usernameLabel = findViewById(R.id.tv_username_label);
        passwordLabel = findViewById(R.id.tv_password_label);
        confirmPasswordLabel = findViewById(R.id.tv_confirm_password_label);

        Button signinButton = findViewById(R.id.btn_sign_in);

        signinButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            resetColors();

            if (email.isEmpty()) {
                emailLabel.setTextColor(Color.RED);
                return;
            }
            if (username.isEmpty()) {
                usernameLabel.setTextColor(Color.RED);
                return;
            }
            if (password.isEmpty()) {
                passwordLabel.setTextColor(Color.RED);
                return;
            }
            if (!password.equals(confirmPassword)) {
                confirmPasswordLabel.setTextColor(Color.RED);
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }


// INSIDE onCreate -> signinButton.setOnClickListener
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // 1. Account Created in Auth, but DO NOT save to database yet.
                            Toast.makeText(this, "Account created. Please set up profile.", Toast.LENGTH_SHORT).show();

                            // 2. Pass data to the next screen
                            Intent intent = new Intent(C_SignInActivity.this, C_SignInProfilePhoto.class);
                            intent.putExtra("USER_EMAIL", email);
                            intent.putExtra("USER_USERNAME", username);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

        });
    }


    private void resetColors() {
        emailLabel.setTextColor(Color.BLACK);
        usernameLabel.setTextColor(Color.BLACK);
        passwordLabel.setTextColor(Color.BLACK);
        confirmPasswordLabel.setTextColor(Color.BLACK);
    }
}
