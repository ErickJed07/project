package com.example.project;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
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

public class CategorySelectionBottomSheet extends BottomSheetDialogFragment {

    public interface CategorySelectionListener {
        void onCategorySelected(String category);
    }

    private CategorySelectionListener listener;
    private String currentCategory = "";
    private String selectedCategory = "";
    private CategoryAdapter adapter;
    private List<CategoryAdapter.CategoryItem> categoryList = new ArrayList<>();

    public static CategorySelectionBottomSheet newInstance(String currentCategory, CategorySelectionListener listener) {
        CategorySelectionBottomSheet fragment = new CategorySelectionBottomSheet();
        fragment.currentCategory = currentCategory;
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_category_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvCategories = view.findViewById(R.id.rv_categories);

        adapter = new CategoryAdapter(categoryList, currentCategory, category -> {
            selectedCategory = category.name;
            if (listener != null) {
                listener.onCategorySelected(selectedCategory);
                dismiss();
            }
        });

        rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvCategories.setAdapter(adapter);

        loadCategories();

        view.findViewById(R.id.iv_add_category).setOnClickListener(v -> showAddCategoryDialog());

        // If something was already selected (e.g. currentCategory), set selectedCategory
        if (currentCategory != null && !currentCategory.isEmpty() && !currentCategory.equals("Select category")) {
            selectedCategory = currentCategory;
        }
    }

    private void loadCategories() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            loadDefaultCategories();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories");

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String name = child.child("name").getValue(String.class);
                        String iconName = child.child("icon").getValue(String.class);
                        if (name != null) {
                            categoryList.add(new CategoryAdapter.CategoryItem(name, getIconRes(iconName != null ? iconName : name)));
                        }
                    }
                }
                
                // If no categories in Firebase, load defaults
                if (categoryList.isEmpty()) {
                    loadDefaultCategories();
                } else {
                    adapter.setSelectedCategory(currentCategory);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadDefaultCategories();
            }
        });
    }

    private void loadDefaultCategories() {
        categoryList.clear();
        categoryList.add(new CategoryAdapter.CategoryItem("Tops", R.drawable.shirt));
        categoryList.add(new CategoryAdapter.CategoryItem("Bottoms", R.drawable.jeans));
        categoryList.add(new CategoryAdapter.CategoryItem("Dresses", R.drawable.dresss));
        categoryList.add(new CategoryAdapter.CategoryItem("Outerwear", R.drawable.jacket));
        categoryList.add(new CategoryAdapter.CategoryItem("Shoes", R.drawable.shoes));
        categoryList.add(new CategoryAdapter.CategoryItem("Accessories", R.drawable.accessory));
        adapter.setSelectedCategory(currentCategory);
        adapter.notifyDataSetChanged();
    }

    private int getIconRes(String iconName) {
        switch (iconName) {
            case "Shirt": return R.drawable.shirt;
            case "Top": return R.drawable.top;
            case "Bottom": return R.drawable.botttom;
            case "Outer": return R.drawable.outer;
            case "Dress": return R.drawable.dresss;
            case "Shoes": return R.drawable.shoes;
            case "Bag": return R.drawable.bag;
            case "Hat": return R.drawable.hat;
            case "Accessories": return R.drawable.accesories;
            case "PreOutfit": return R.drawable.preoutfit;
            case "Tops": return R.drawable.shirt;
            case "Bottoms": return R.drawable.jeans;
            case "Dresses": return R.drawable.dresss;
            case "Outerwear": return R.drawable.jacket;
            default: return R.drawable.shirt;
        }
    }

    private void showAddCategoryDialog() {
        if (getContext() == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_category, null);
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
        rvIcons.setLayoutManager(new GridLayoutManager(getContext(), 4));
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
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) return;
        
        String uid = mAuth.getCurrentUser().getUid();
        String id = name.replaceAll("[.#$\\[\\]]", "");

        if (id.isEmpty()) {
            Toast.makeText(getContext(), "Invalid Name", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories").child(id);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("name", name);
        data.put("icon", icon);

        ref.setValue(data).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Category added", Toast.LENGTH_SHORT).show();
            loadCategories(); // Refresh list
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to add category", Toast.LENGTH_SHORT).show();
        });
    }
}
