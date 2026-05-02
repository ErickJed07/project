package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ColorListFragment extends Fragment {

    private static final String ARG_IS_MULTIPLE = "is_multiple";
    private static final String ARG_COLORS = "colors";

    private boolean isMultipleMode;
    private List<ColorOption> colors;
    private ColorAdapter adapter;

    public static ColorListFragment newInstance(boolean isMultiple, ArrayList<ColorOption> colors) {
        ColorListFragment fragment = new ColorListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_MULTIPLE, isMultiple);
        args.putSerializable(ARG_COLORS, colors);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isMultipleMode = getArguments().getBoolean(ARG_IS_MULTIPLE);
            colors = (List<ColorOption>) getArguments().getSerializable(ARG_COLORS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        recyclerView.setPadding(16, 16, 16, 16);
        recyclerView.setClipToPadding(false);
        return recyclerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        
        adapter = new ColorAdapter(colors, isMultipleMode, color -> {
            if (!isMultipleMode) {
                // Clear others
                for (ColorOption co : colors) {
                    co.setSelected(false);
                }
                color.setSelected(true);
            } else {
                color.setSelected(!color.isSelected());
            }
            adapter.notifyDataSetChanged();
            
            // Notify activity if needed, or just let the Apply button handle it
            if (getActivity() instanceof ColorSelectionBottomSheet.ColorSelectionListener) {
                // We can update the selection list in the bottom sheet
            }
        });
        recyclerView.setAdapter(adapter);
    }
}