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

        // If something was already selected (e.g. currentCategory), set selectedCategory
        if (currentCategory != null && !currentCategory.isEmpty() && !currentCategory.equals("Select category")) {
            selectedCategory = currentCategory;
        }
    }

    private void loadCategories() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("gender")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String gender = snapshot.getValue(String.class);
                    boolean isWoman = !"man".equals(gender);
                    
                    categoryList.clear();
                    for (CategoryManager.CategoryItem item : CategoryManager.getCategories(isWoman)) {
                        categoryList.add(new CategoryAdapter.CategoryItem(item.id, item.name, item.iconRes));
                    }
                    adapter.setSelectedCategory(currentCategory);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Fallback to default
                    categoryList.clear();
                    for (CategoryManager.CategoryItem item : CategoryManager.getCategories()) {
                        categoryList.add(new CategoryAdapter.CategoryItem(item.id, item.name, item.iconRes));
                    }
                    adapter.notifyDataSetChanged();
                }
            });
    }
}
