// This is the Home Screen of the app
// It has 2 buttons: "Login" and "Sign In"
// If you click "Login" it goes to the Login page
// If you click "Sign In" it goes to the Sign In page

package com.example.project;

import android.content.Intent; // Lets us move from one screen to another
import android.os.Bundle; // Helps the screen get ready when it opens
import android.view.View; // Lets us notice when someone taps a button
import android.widget.Button; // Lets us use buttons
import androidx.appcompat.app.AppCompatActivity; // Basic screen stuff

public class A_HomeActivity extends AppCompatActivity {

    // These are the buttons on the screen
    Button loginButton, signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This tells the app to show the design called "home"
        setContentView(R.layout.a_home);

        // Connect the buttons in code to the buttons on the screen
        loginButton = findViewById(R.id.loginbutton);
        signInButton = findViewById(R.id.signinbutton);

        // When someone clicks the Login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to the Login page
                Intent intent = new Intent(A_HomeActivity.this, B_LoginActivity.class);
                startActivity(intent); // Open the Login page
            }
        });

        // When someone clicks the Sign In button
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to the Sign In page
                Intent intent = new Intent(A_HomeActivity.this, C_SignInActivity.class);
                startActivity(intent); // Open the Sign In page
            }
        });
    }
}
