package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ColorSelectionBottomSheet extends BottomSheetDialogFragment {

    public interface ColorSelectionListener {
        void onColorsSelected(List<ColorOption> selectedColors, boolean isMultiple);
    }

    private ColorSelectionListener listener;
    private boolean isMultipleMode = true;
    private final List<ColorOption> selectedColors = new ArrayList<>();

    public static ColorSelectionBottomSheet newInstance(ColorSelectionListener listener) {
        ColorSelectionBottomSheet fragment = new ColorSelectionBottomSheet();
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_color_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager);
        View btnApply = view.findViewById(R.id.btn_apply);

        List<String> categories = Arrays.asList("Basic", "Neutral", "Warm", "Cool");
        
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return ColorListFragment.newInstance(isMultipleMode, getColorsForCategory(categories.get(position)));
            }

            @Override
            public int getItemCount() {
                return categories.size();
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(categories.get(position))).attach();

        btnApply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onColorsSelected(selectedColors, isMultipleMode);
            }
            dismiss();
        });
    }

    public void toggleColorSelection(ColorOption color) {
        if (!isMultipleMode) {
            selectedColors.clear();
            selectedColors.add(color);
        } else {
            if (color.isSelected()) {
                if (!selectedColors.contains(color)) {
                    selectedColors.add(color);
                }
            } else {
                selectedColors.remove(color);
            }
        }
    }

    private ArrayList<ColorOption> getColorsForCategory(String category) {
        ArrayList<ColorOption> colors = new ArrayList<>();
        switch (category) {
            case "Basic":
                colors.add(new ColorOption("Black", "#000000"));
                colors.add(new ColorOption("White", "#FFFFFF"));
                colors.add(new ColorOption("Red", "#FF0000"));
                colors.add(new ColorOption("Blue", "#0000FF"));
                colors.add(new ColorOption("Green", "#00FF00"));
                colors.add(new ColorOption("Yellow", "#FFFF00"));
                break;
            case "Neutral":
                colors.add(new ColorOption("Grey", "#808080"));
                colors.add(new ColorOption("Beige", "#F5F5DC"));
                colors.add(new ColorOption("Brown", "#A52A2A"));
                colors.add(new ColorOption("Navy", "#000080"));
                break;
            case "Warm":
                colors.add(new ColorOption("Orange", "#FFA500"));
                colors.add(new ColorOption("Pink", "#FFC0CB"));
                colors.add(new ColorOption("Purple", "#800080"));
                colors.add(new ColorOption("Magenta", "#FF00FF"));
                break;
            case "Cool":
                colors.add(new ColorOption("Cyan", "#00FFFF"));
                colors.add(new ColorOption("Teal", "#008080"));
                colors.add(new ColorOption("Lavender", "#E6E6FA"));
                colors.add(new ColorOption("Mint", "#98FF98"));
                break;
        }
        return colors;
    }
}
