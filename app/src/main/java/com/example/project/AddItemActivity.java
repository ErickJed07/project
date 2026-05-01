package com.example.project;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class AddItemActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        TextView tvSize = findViewById(R.id.tv_size);
        ChipGroup cgOccasions = findViewById(R.id.cg_occasions);

        findViewById(R.id.cv_close).setOnClickListener(v -> finish());

        findViewById(R.id.rl_size_dropdown).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(AddItemActivity.this, v);
            String[] sizes = {"XXS", "XS", "S", "M", "L", "XL", "XXL"};
            for (String size : sizes) {
                popup.getMenu().add(size);
            }
            popup.setOnMenuItemClickListener(item -> {
                tvSize.setText(item.getTitle());
                tvSize.setTextColor(ContextCompat.getColor(this, R.color.wardrobe_text_primary));
                return true;
            });
            popup.show();
        });

        findViewById(R.id.chip_add_occasion).setOnClickListener(v -> showAddOccasionDialog(cgOccasions));

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            Toast.makeText(this, "Item Saved Successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
        
        findViewById(R.id.ll_take_photo).setOnClickListener(v -> {
            Toast.makeText(this, "Opening Camera...", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.ll_from_gallery).setOnClickListener(v -> {
            Toast.makeText(this, "Opening Gallery...", Toast.LENGTH_SHORT).show();
        });
    }

    private void showAddOccasionDialog(ChipGroup chipGroup) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Occasion");

        final EditText input = new EditText(this);
        input.setHint("e.g. Wedding");
        input.setPadding(64, 32, 64, 32);
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                addNewOccasionChip(chipGroup, name);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addNewOccasionChip(ChipGroup chipGroup, String name) {
        // Inflate from the exact same XML template to ensure 100% style matching
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_occasion_chip, chipGroup, false);
        chip.setText(name);
        
        // LONG PRESS TO DELETE - only for dynamically added ones
        chip.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Delete Occasion")
                .setMessage("Are you sure you want to delete '" + name + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    chipGroup.removeView(chip);
                    Toast.makeText(this, "Occasion deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        });
        
        // Add before the "+" button
        int childCount = chipGroup.getChildCount();
        chipGroup.addView(chip, childCount - 1);
    }
}