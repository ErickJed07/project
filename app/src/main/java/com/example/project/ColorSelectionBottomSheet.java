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
import java.util.List;

public class ColorSelectionBottomSheet extends BottomSheetDialogFragment {

    public interface ColorSelectionListener {
        void onColorsSelected(List<ColorOption> selectedColors, boolean isMultiple);
    }

    private ColorSelectionListener listener;
    private ArrayList<ColorOption> singleModeColors;
    private ArrayList<ColorOption> multipleModeColors;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

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

        initColors();

        viewPager = view.findViewById(R.id.vp_color_pager);
        tabLayout = view.findViewById(R.id.tl_color_tabs);
        View btnApply = view.findViewById(R.id.btn_apply_colors);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return ColorListFragment.newInstance(false, singleModeColors);
                } else {
                    return ColorListFragment.newInstance(true, multipleModeColors);
                }
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Single" : "Multiple");
        }).attach();

        btnApply.setOnClickListener(v -> {
            if (listener != null) {
                int currentTab = viewPager.getCurrentItem();
                List<ColorOption> selected = new ArrayList<>();
                if (currentTab == 0) {
                    for (ColorOption co : singleModeColors) {
                        if (co.isSelected()) selected.add(co);
                    }
                } else {
                    for (ColorOption co : multipleModeColors) {
                        if (co.isSelected()) selected.add(co);
                    }
                }
                listener.onColorsSelected(selected, currentTab == 1);
            }
            dismiss();
        });
    }

    private void initColors() {
        String[][] colorData = {
            {"Black", "#000000"}, {"White", "#FFFFFF"}, {"Red", "#FF0000"},
            {"Green", "#008000"}, {"Yellow", "#FFFF00"}, {"Blue", "#0000FF"},
            {"Brown", "#A52A2A"}, {"Purple", "#800080"}, {"Pink", "#FFC0CB"},
            {"Orange", "#FFA500"}, {"Grey", "#808080"}
        };

        singleModeColors = new ArrayList<>();
        multipleModeColors = new ArrayList<>();

        for (String[] data : colorData) {
            singleModeColors.add(new ColorOption(data[0], data[1]));
            multipleModeColors.add(new ColorOption(data[0], data[1]));
        }
    }
}