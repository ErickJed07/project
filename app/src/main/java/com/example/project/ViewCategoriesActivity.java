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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wardrobe_categories);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Users");

        rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new WardrobeCategoryAdapter(this, categoryList);
        adapter.setOnCategoryLongClickListener(category -> {
            showDeleteConfirmation(category);
        });
        rvCategories.setAdapter(adapter);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        
        findViewById(R.id.iv_add_category).setOnClickListener(v -> showAddCategoryDialog());

        findViewById(R.id.camera_menu).setOnClickListener(v -> {
            startActivity(new Intent(this, AddItemActivity.class));
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

    private void loadCategories() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child(uid).child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                int totalItems = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String id = child.getKey();
                    String name = child.child("name").getValue(String.class);
                    if (name == null) name = id;
                    
                    String icon = child.child("icon").getValue(String.class);
                    
                    long itemCount = 0;
                    if (child.hasChild("photos")) {
                        itemCount = child.child("photos").getChildrenCount();
                        totalItems += itemCount;
                    }
                    
                    categoryList.add(new CategoryModel(id, name, icon, (int) itemCount));
                }
                
                // Inject "All Clothes" at the top
                categoryList.add(0, new CategoryModel("all_clothes", "All Clothes", "All Clothes", totalItems));

                adapter.updateList(new ArrayList<>(categoryList));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewCategoriesActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etName = dialogView.findViewById(R.id.et_category_name);
        RecyclerView rvIcons = dialogView.findViewById(R.id.rv_icons);
        View btnAdd = dialogView.findViewById(R.id.btn_add);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);

        List<IconItem> icons = getAvailableIcons();
        final String[] selectedIcon = {icons.get(0).name}; // Default selection

        IconSelectionAdapter iconAdapter = new IconSelectionAdapter(icons, iconName -> selectedIcon[0] = iconName);
        rvIcons.setLayoutManager(new GridLayoutManager(this, 4));
        rvIcons.setAdapter(iconAdapter);

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Name required");
                return;
            }
            saveCategoryToFirebase(name, selectedIcon[0]);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private List<IconItem> getAvailableIcons() {
        List<IconItem> list = new ArrayList<>();
        list.add(new IconItem("Shirt", R.drawable.shirt));
        list.add(new IconItem("Top", R.drawable.top));
        list.add(new IconItem("Bottom", R.drawable.botttom));
        list.add(new IconItem("Outer", R.drawable.outer));
        list.add(new IconItem("Dress", R.drawable.dresss));
        list.add(new IconItem("Shoes", R.drawable.shoes));
        list.add(new IconItem("Bag", R.drawable.bag));
        list.add(new IconItem("Hat", R.drawable.hat));
        list.add(new IconItem("Accessories", R.drawable.accesories));
        list.add(new IconItem("PreOutfit", R.drawable.preoutfit));
        return list;
    }

    private void saveCategoryToFirebase(String name, String icon) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        String id = name.replaceAll("[.#$\\[\\]]", "");

        if (id.isEmpty()) {
            Toast.makeText(this, "Invalid Name", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = dbRef.child(uid).child("categories").child(id);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("name", name);
        data.put("icon", icon);

        ref.setValue(data).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to add category", Toast.LENGTH_SHORT).show();
        });
    }

    private void showDeleteConfirmation(CategoryModel category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_dialog_message);
        View btnDelete = dialogView.findViewById(R.id.btn_delete);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);

        tvTitle.setText("Delete Category?");
        tvMessage.setText("Are you sure you want to delete '" + category.name + "' and all its items? This action cannot be undone.");

        btnDelete.setOnClickListener(v -> {
            deleteCategory(category);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void deleteCategory(CategoryModel category) {
        if (mAuth.getCurrentUser() == null) return;
        if ("all_clothes".equals(category.id)) {
            Toast.makeText(this, "Cannot delete 'All Clothes' category", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = mAuth.getCurrentUser().getUid();
        
        dbRef.child(uid).child("categories").child(category.id).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete category", Toast.LENGTH_SHORT).show();
                });
    }

    // Simple model class
    public static class CategoryModel {
        public String id;
        public String name;
        public String icon;
        public int itemCount;

        public CategoryModel(String id, String name, int itemCount) {
            this(id, name, null, itemCount);
        }

        public CategoryModel(String id, String name, String icon, int itemCount) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.itemCount = itemCount;
        }
    }
}
