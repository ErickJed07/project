// This is the Login Screen of the app
// You can type your Email and Password here
// Click the Login button to go inside the app
// If something is missing, it will show red and tell you what’s wrong

package com.example.project;

import android.content.Intent; // Lets us move from this screen to another screen
import android.graphics.Color; // Lets us change colors, like red for mistakes
import android.os.Bundle; // Helps the screen get ready when it opens
import android.widget.Button; // Lets us use buttons
import android.widget.EditText; // Lets us type stuff like email and password
import android.widget.TextView; // Lets us show labels or words
import android.widget.Toast; // Shows little popup messages

import androidx.appcompat.app.AppCompatActivity; // Basic screen stuff

import com.google.firebase.auth.FirebaseAuth; // Helps to check your email/password
import com.google.firebase.auth.FirebaseUser; // Stores info about the person who logs in

public class B_LoginActivity extends AppCompatActivity {

    // These are the places to type email and password
    private EditText emailInput, passwordInput;
    // These are the labels that say "Email" and "Password"
    private TextView emailLabel, passwordLabel;
    // This is Firebase, it checks login info
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Show the design called "login"
        setContentView(R.layout.b_login);

        // Connect to Firebase so we can check login
        mAuth = FirebaseAuth.getInstance();

        // Connect the boxes where you type email and password
        emailInput = findViewById(R.id.email_info);
        passwordInput = findViewById(R.id.pass_info);

        // Connect the labels for email and password
        emailLabel = findViewById(R.id.email);
        passwordLabel = findViewById(R.id.pass);

        // Connect the login button
        Button loginButton = findViewById(R.id.loginbutton);

        // What happens when someone clicks Login
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim(); // Get email typed
            String password = passwordInput.getText().toString().trim(); // Get password typed

            // Make labels black again (in case they were red before)
            resetColors();

            // Check if email is empty
            if (email.isEmpty()) {
                emailLabel.setTextColor(Color.RED); // Turn label red
                Toast.makeText(this, "Enter your Email", Toast.LENGTH_SHORT).show(); // Show popup
                return; // Stop here, wait for user to type
            }
            // Check if password is empty
            if (password.isEmpty()) {
                passwordLabel.setTextColor(Color.RED); // Turn label red
                Toast.makeText(this, "Enter your Password", Toast.LENGTH_SHORT).show(); // Show popup
                return; // Stop here, wait for user to type
            }

            // Check email and password in Firebase (login check)
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) { // If login is correct
                            FirebaseUser user = mAuth.getCurrentUser(); // Get logged-in user
                            if (user != null) {
                                // Show a message and go to Feed Screen
                                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(B_LoginActivity.this, D_FeedActivity.class));
                                finish(); // Close Login Screen
                            }
                        } else { // If login is wrong
                            Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    // Make labels black again (reset colors)
    private void resetColors() {
        emailLabel.setTextColor(Color.BLACK);
        passwordLabel.setTextColor(Color.BLACK);
    }
}
