package com.example.project;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class I1_ProfileActivity extends AppCompatActivity {

    private TextView uploadProf, likedProf, favProf, usernameText, emailText;
    private FrameLayout contentFrame;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i1_profile);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Find views
        usernameText = findViewById(R.id.username);
        emailText = findViewById(R.id.email_profile);

        if (currentUser != null) {
            String uid = currentUser.getUid();
            String email = currentUser.getEmail();
            emailText.setText(email);

            // Fetch username from Firebase Realtime Database
            databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            databaseRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String username = snapshot.getValue(String.class);
                        usernameText.setText(username);
                    } else {
                        usernameText.setText("Unknown User");
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(I1_ProfileActivity.this, "Failed to load username", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Initialize other views
        contentFrame = findViewById(R.id.Content);
        uploadProf = findViewById(R.id.upload_prof);
        likedProf = findViewById(R.id.liked_prof);
        favProf = findViewById(R.id.fav_prof);

        // Load default content
        loadContent(new I2_Profile_UploadContent());
        setUnderline(uploadProf, true);
        setUnderline(likedProf, false);
        setUnderline(favProf, false);

        uploadProf.setOnClickListener(v -> {
            loadContent(new I2_Profile_UploadContent());
            setUnderline(uploadProf, true);
            setUnderline(likedProf, false);
            setUnderline(favProf, false);
        });

        likedProf.setOnClickListener(v -> {
            loadContent(new I4_Profile_LikedContent());
            setUnderline(uploadProf, false);
            setUnderline(likedProf, true);
            setUnderline(favProf, false);
        });

        favProf.setOnClickListener(v -> {
            loadContent(new I5_Profile_FavContent());
            setUnderline(uploadProf, false);
            setUnderline(likedProf, false);
            setUnderline(favProf, true);
        });
    }

    private void loadContent(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.Content, fragment)
                .commit();
    }

    private void setUnderline(TextView textView, boolean isUnderlined) {
        if (isUnderlined) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        }
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();

        if (viewId == R.id.home_menu) {
            intent = new Intent(this, D1_FeedActivity.class);
        } else if (viewId == R.id.calendar_menu) {
            intent = new Intent(this, E1_CalendarActivity.class);
        } else if (viewId == R.id.add_menu) {
            intent = new Intent(this, F1_CameraActivity.class);
        } else if (viewId == R.id.closet_menu) {
            intent = new Intent(this, G1_ClosetActivity.class);
        } else if (viewId == R.id.profile_menu) {
            return; // already in profile
        }

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
}
