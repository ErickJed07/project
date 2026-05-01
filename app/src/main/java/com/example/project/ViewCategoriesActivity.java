package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ViewCategoriesActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private WardrobeCategoryAdapter adapter;
    private List<CategoryModel> categoryList = new ArrayList<>();
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_categories);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Users");

        rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new WardrobeCategoryAdapter(this, categoryList);
        rvCategories.setAdapter(adapter);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        
        findViewById(R.id.iv_add_category).setOnClickListener(v -> {
            // Re-using the logic from G1_ClosetActivity or just navigating there
            Intent intent = new Intent(this, G1_ClosetActivity.class);
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
                for (DataSnapshot child : snapshot.getChildren()) {
                    String id = child.getKey();
                    String name = child.child("name").getValue(String.class);
                    if (name == null) name = id;
                    
                    long itemCount = 0;
                    if (child.hasChild("photos")) {
                        itemCount = child.child("photos").getChildrenCount();
                    }
                    
                    categoryList.add(new CategoryModel(id, name, (int) itemCount));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewCategoriesActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Simple model class
    public static class CategoryModel {
        public String id;
        public String name;
        public int itemCount;

        public CategoryModel(String id, String name, int itemCount) {
            this.id = id;
            this.name = name;
            this.itemCount = itemCount;
        }
    }
}
