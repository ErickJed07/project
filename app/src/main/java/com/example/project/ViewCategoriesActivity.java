package com.example.project;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewCategoriesActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private WardrobeCategoryAdapter adapter;
    private List<CategoryModel> categoryList = new ArrayList<>();
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private boolean isWomanSelected = true;
    private TextView tvWoman, tvMan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wardrobe_categories);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Users");

        tvWoman = findViewById(R.id.tv_woman);
        tvMan = findViewById(R.id.tv_man);

        rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new WardrobeCategoryAdapter(this, categoryList);
        rvCategories.setAdapter(adapter);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        
        findViewById(R.id.camera_menu).setOnClickListener(v -> {
            startActivity(new Intent(this, AddItemActivity.class));
        });

        tvWoman.setOnClickListener(v -> {
            if (!isWomanSelected) {
                isWomanSelected = true;
                updateToggleUI();
                loadCategories();
            }
        });

        tvMan.setOnClickListener(v -> {
            if (isWomanSelected) {
                isWomanSelected = false;
                updateToggleUI();
                loadCategories();
            }
        });

        EditText etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadCategories();
    }

    private void updateToggleUI() {
        if (isWomanSelected) {
            tvWoman.setBackgroundResource(R.drawable.bg_pill);
            tvWoman.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ai_accent)));
            tvWoman.setTextColor(Color.WHITE);
            tvWoman.setTypeface(null, android.graphics.Typeface.BOLD);

            tvMan.setBackground(null);
            tvMan.setTextColor(ContextCompat.getColor(this, R.color.ai_chip_unselected_text));
            tvMan.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            tvMan.setBackgroundResource(R.drawable.bg_pill);
            tvMan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ai_accent)));
            tvMan.setTextColor(Color.WHITE);
            tvMan.setTypeface(null, android.graphics.Typeface.BOLD);

            tvWoman.setBackground(null);
            tvWoman.setTextColor(ContextCompat.getColor(this, R.color.ai_chip_unselected_text));
            tvWoman.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void loadCategories() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child(uid).child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                int totalItems = 0;
                
                // Add fixed categories from CategoryManager
                for (CategoryManager.CategoryItem fixedItem : CategoryManager.getCategories(isWomanSelected)) {
                    long itemCount = 0;
                    DataSnapshot categorySnapshot = snapshot.child(fixedItem.id);
                    if (categorySnapshot.exists() && categorySnapshot.hasChild("photos")) {
                        itemCount = categorySnapshot.child("photos").getChildrenCount();
                        totalItems += itemCount;
                    }
                    categoryList.add(new CategoryModel(fixedItem.id, fixedItem.name, fixedItem.iconRes, (int) itemCount));
                }
                
                // Inject "All Clothes" at the top
                categoryList.add(0, new CategoryModel("all_clothes", "All Clothes", R.drawable.hanger, totalItems));

                adapter.updateList(new ArrayList<>(categoryList));
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
        public int iconRes;
        public int itemCount;

        public CategoryModel(String id, String name, int itemCount) {
            this(id, name, 0, itemCount);
        }

        public CategoryModel(String id, String name, int iconRes, int itemCount) {
            this.id = id;
            this.name = name;
            this.iconRes = iconRes;
            this.itemCount = itemCount;
        }
    }
}
