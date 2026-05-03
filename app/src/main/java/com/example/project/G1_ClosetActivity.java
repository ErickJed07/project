package com.example.project;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class G1_ClosetActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private FirebaseStorage storage;

    private GridLayout gridLayout;
    private View addButtonView;
    private final Set<String> existingCategoryIds = new HashSet<>();
    private final Map<String, View> categoryViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.g1_closet);

        // --- NEW CODE: Handle Back Button to go to Feed ---
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(G1_ClosetActivity.this, D_FeedActivity.class);
                // Clear stack so the user can't go "back" to the closet easily
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });



        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Users");
        storage = FirebaseStorage.getInstance();

        gridLayout = findViewById(R.id.galleryGrid);
        gridLayout.setColumnCount(3);

        // addButtonView = findViewById(R.id.AddCategory);
        // addButtonView.setVisibility(View.GONE);

        findViewById(R.id.newoutfit).setOnClickListener(v -> {
            Intent intent = new Intent(G1_ClosetActivity.this, G7_NewOutfitActivity.class);
            startActivity(intent);
        });

        initializeFixedCategories();
        loadCategoriesFromFirebase();
    }

    private void loadCategoriesFromFirebase() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child(uid).child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (View view : categoryViews.values()) {
                    gridLayout.removeView(view);
                }
                categoryViews.clear();
                existingCategoryIds.clear();

                // Add fixed categories
                for (CategoryManager.CategoryItem item : CategoryManager.getCategories()) {
                    DataSnapshot child = snapshot.child(item.id);
                    processCategorySnapshot(child, item);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }

    private void processCategorySnapshot(DataSnapshot child, CategoryManager.CategoryItem fixedItem) {
        String categoryId = fixedItem.id;
        String name = fixedItem.name;
        String firstImageUrl = "";

        if (child.exists() && child.hasChild("photos")) {
            for (DataSnapshot photoSnap : child.child("photos").getChildren()) {
                if (photoSnap.hasChild("imageUrl")) {
                    String url = photoSnap.child("imageUrl").getValue(String.class);
                    if (url != null && !url.isEmpty()) {
                        firstImageUrl = url;
                        break;
                    }
                }
            }
        }

        addCategoryToUI(categoryId, name, firstImageUrl);
    }



    private void addCategoryToUI(String categoryId, String categoryName, String imageUrl) {
        if (existingCategoryIds.contains(categoryId)) return;
        existingCategoryIds.add(categoryId);

        float scale = getResources().getDisplayMetrics().density;
        int cardSize = (int) (100 * scale);

        CardView card = new CardView(this);
        card.setCardElevation(6f);
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(getResources().getColor(R.color.white));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(0, 0, 0, 0);
        card.setLayoutParams(params);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 20, 0, 0);
        container.setGravity(Gravity.CENTER);

        ImageView imagePreview = new ImageView(this);
        imagePreview.setLayoutParams(new FrameLayout.LayoutParams(cardSize, cardSize));
        imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagePreview.setClipToOutline(true);


        // --- UPDATED LOGIC: Use Fixed Category Icon ---
        CategoryManager.CategoryItem fixed = CategoryManager.getCategoryById(categoryId);
        if (fixed != null) {
            imagePreview.setImageResource(fixed.iconRes);
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder_2)
                    .into(imagePreview);
        } else {
            imagePreview.setImageResource(R.drawable.ic_placeholder_2);
        }

        // ----------------------------------------------------------------

        TextView label = new TextView(this);
        label.setText(categoryName);
        label.setTextColor(getResources().getColor(R.color.black));
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextSize(14f);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, 0, 0, 0);

        container.addView(imagePreview);
        container.addView(label);
        card.addView(container);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, G2_Closet_CategoryActivity.class);
            intent.putExtra("CATEGORY_NAME", categoryName);
            intent.putExtra("CATEGORY_ID", categoryId);
            startActivity(intent);
        });

        // card.setOnLongClickListener(v -> {
        //     return true;
        // });

        gridLayout.addView(card);
        categoryViews.put(categoryId, card);
    }



    private void initializeFixedCategories() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference categoriesRef = dbRef.child(uid).child("categories");

        for (CategoryManager.CategoryItem item : CategoryManager.getCategories()) {
            DatabaseReference specificCatRef = categoriesRef.child(item.id);
            Map<String, Object> catData = new HashMap<>();
            catData.put("id", item.id);
            catData.put("name", item.name);
            specificCatRef.updateChildren(catData);
        }
    }
    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();
        if (viewId == R.id.home_menu) intent = new Intent(this, D_FeedActivity.class);
        else if (viewId == R.id.calendar_menu) intent = new Intent(this, E_CalendarActivity.class);
        else if (viewId == R.id.camera_menu) intent = new Intent(this, F1_CameraActivity.class);
        else if (viewId == R.id.closet_menu) intent = new Intent(this, G1_ClosetActivity.class);
        else if (viewId == R.id.profile_menu) intent = new Intent(this, I_ProfileActivity.class);

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
}
