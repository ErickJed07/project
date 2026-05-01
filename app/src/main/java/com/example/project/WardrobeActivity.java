package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class WardrobeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wardrobe);

        findViewById(R.id.tv_view_categories).setOnClickListener(v -> {
            Intent intent = new Intent(WardrobeActivity.this, ViewCategoriesActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.tv_view_calendar).setOnClickListener(v -> {
            Intent intent = new Intent(WardrobeActivity.this, E_CalendarActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.fab_add).setOnClickListener(v -> {
            Intent intent = new Intent(WardrobeActivity.this, G1_ClosetActivity.class);
            startActivity(intent);
        });
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();
        if (viewId == R.id.home_menu) intent = new Intent(this, D_FeedActivity.class);
        else if (viewId == R.id.calendar_menu) intent = new Intent(this, E_CalendarActivity.class);
        else if (viewId == R.id.camera_menu) intent = new Intent(this, F1_CameraActivity.class);
        else if (viewId == R.id.closet_menu) intent = new Intent(this, G1_ClosetActivity.class);
        else if (viewId == R.id.profile_menu) intent = new Intent(this, I_ProfileActivity.class);
        else if (viewId == R.id.wardrobe_menu) intent = new Intent(this, WardrobeActivity.class);
        else if (viewId == R.id.ai_menu) intent = new Intent(this, AiActivity.class);
        if (intent != null) { startActivity(intent); finish(); }
    }
}
//check