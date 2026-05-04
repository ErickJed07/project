package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class WardrobeActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private WardrobeCategoryHorizontalAdapter adapter;
    private List<ViewCategoriesActivity.CategoryModel> categoryList = new ArrayList<>();
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private TextView tvTotalItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wardrobe);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Users");

        tvTotalItems = findViewById(R.id.tv_total_items_val);
        rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        adapter = new WardrobeCategoryHorizontalAdapter(this, categoryList);
        rvCategories.setAdapter(adapter);

        findViewById(R.id.tv_view_categories).setOnClickListener(v -> {
            Intent intent = new Intent(WardrobeActivity.this, ViewCategoriesActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.tv_view_calendar).setOnClickListener(v -> {
            Intent intent = new Intent(WardrobeActivity.this, E_CalendarActivity.class);
            startActivity(intent);
        });

        loadCategories();
    }

    private void loadCategories() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child(uid).child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                int totalItems = 0;
                
                // Calculate total items and load categories
                for (CategoryManager.CategoryItem fixedItem : CategoryManager.getCategories(true)) {
                    long itemCount = 0;
                    DataSnapshot categorySnapshot = snapshot.child(fixedItem.id);
                    if (categorySnapshot.exists() && categorySnapshot.hasChild("photos")) {
                        itemCount = categorySnapshot.child("photos").getChildrenCount();
                        totalItems += itemCount;
                    }
                    categoryList.add(new ViewCategoriesActivity.CategoryModel(fixedItem.id, fixedItem.name, fixedItem.iconRes, (int) itemCount));
                }

                if (tvTotalItems != null) {
                    tvTotalItems.setText(String.valueOf(totalItems));
                }
                adapter.updateList(new ArrayList<>(categoryList));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(WardrobeActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();
        if (viewId == R.id.home_menu) intent = new Intent(this, D_FeedActivity.class);
        else if (viewId == R.id.closet_menu) intent = new Intent(this, G1_ClosetActivity.class);
        else if (viewId == R.id.profile_menu) intent = new Intent(this, I_ProfileActivity.class);
        else if (viewId == R.id.wardrobe_menu) intent = new Intent(this, WardrobeActivity.class);
        else if (viewId == R.id.ai_menu) intent = new Intent(this, AiActivity.class);
        if (intent != null) { startActivity(intent); finish(); }
    }
}
//check