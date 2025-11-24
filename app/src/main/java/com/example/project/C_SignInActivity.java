// This is the Sign In / Registration Screen of the app
// You can make a new account here by typing Email, Username, Password, and Confirm Password
// It checks if everything is typed correctly and saves your info
// After successful registration, it takes you to the Login page

package com.example.project;

import android.content.Intent; // Lets us go from this screen to another screen
import android.graphics.Color; // Lets us change colors (red for mistakes)
import android.os.Bundle; // Helps the screen get ready when it opens
import android.widget.Button; // Lets us use buttons
import android.widget.EditText; // Lets us type info
import android.widget.TextView; // Lets us show labels
import android.widget.Toast; // Shows small popup messages

import androidx.appcompat.app.AppCompatActivity; // Basic screen stuff

import com.google.firebase.auth.FirebaseAuth; // Helps to make accounts and login
import com.google.firebase.database.DatabaseReference; // Helps save info to Firebase
import com.google.firebase.database.FirebaseDatabase; // Database to store users

import java.util.HashMap; // Helps organize info
import java.util.Map; // Helps organize info

public class C_SignInActivity extends AppCompatActivity {

    // Boxes where you type info
    private EditText emailInput, usernameInput, passwordInput, confirmPasswordInput;
    // Labels that say what each box is
    private TextView emailLabel, usernameLabel, passwordLabel, confirmPasswordLabel;
    // Firebase for registration
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Show the design called "signin"
        setContentView(R.layout.c_signin);

        // Connect to Firebase
        mAuth = FirebaseAuth.getInstance();

        // Connect the typing boxes
        emailInput = findViewById(R.id.email_info);
        usernameInput = findViewById(R.id.create_user_info);
        passwordInput = findViewById(R.id.create_pass_info);
        confirmPasswordInput = findViewById(R.id.confirm_pass_info);

        // Connect the labels
        emailLabel = findViewById(R.id.email);
        usernameLabel = findViewById(R.id.create_user);
        passwordLabel = findViewById(R.id.create_pass);
        confirmPasswordLabel = findViewById(R.id.confirm_pass);

        // Connect the Sign In button
        Button signinButton = findViewById(R.id.signinbutton);

        // What happens when you click Sign In
        signinButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim(); // Get typed email
            String username = usernameInput.getText().toString().trim(); // Get typed username
            String password = passwordInput.getText().toString().trim(); // Get typed password
            String confirmPassword = confirmPasswordInput.getText().toString().trim(); // Get confirm password

            // Make all labels black again (in case red before)
            resetColors();

            // Check if boxes are empty and show red + popup if needed
            if (email.isEmpty()) {
                emailLabel.setTextColor(Color.RED);
                Toast.makeText(this, "Enter Your Email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (username.isEmpty()) {
                usernameLabel.setTextColor(Color.RED);
                Toast.makeText(this, "Enter Your Username", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty()) {
                passwordLabel.setTextColor(Color.RED);
                Toast.makeText(this, "Enter Your Password", Toast.LENGTH_SHORT).show();
                return;
            }
            if (confirmPassword.isEmpty()) {
                confirmPasswordLabel.setTextColor(Color.RED);
                Toast.makeText(this, "Enter Confirm Password", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) { // Check if passwords match
                confirmPasswordLabel.setTextColor(Color.RED);
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Make new account in Firebase
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) { // Account created
                            String uid = mAuth.getCurrentUser().getUid(); // Get your unique ID

                            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Users"); // Users database

                            // Save your Email and Username in database
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("email", email);
                            userMap.put("username", username);

                            // Save the info under your unique ID
                            databaseRef.child(uid).setValue(userMap)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) { // Info saved
                                            Toast.makeText(this, "Account created and saved", Toast.LENGTH_SHORT).show();
                                            // Go to Login page
                                            startActivity(new Intent(C_SignInActivity.this, B_LoginActivity.class));
                                            finish();
                                        } else { // Error saving info
                                            Toast.makeText(this, "Failed to save user: " + dbTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                        } else { // Error creating account
                            Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    // Make all labels black again
    private void resetColors() {
        emailLabel.setTextColor(Color.BLACK);
        usernameLabel.setTextColor(Color.BLACK);
        passwordLabel.setTextColor(Color.BLACK);
        confirmPasswordLabel.setTextColor(Color.BLACK);
    }
}
