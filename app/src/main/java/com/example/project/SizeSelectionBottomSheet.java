package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.Arrays;
import java.util.List;

public class SizeSelectionBottomSheet extends BottomSheetDialogFragment {

    public interface SizeSelectionListener {
        void onSizeSelected(String size);
    }

    private SizeSelectionListener listener;
    private String currentSize = "";
    private String selectedSize = "";

    public static SizeSelectionBottomSheet newInstance(String currentSize, SizeSelectionListener listener) {
        SizeSelectionBottomSheet fragment = new SizeSelectionBottomSheet();
        fragment.currentSize = currentSize;
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_size_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvSizes = view.findViewById(R.id.rv_sizes);

        List<String> sizeList = Arrays.asList("XXS", "XS", "S", "M", "L", "XL", "XXL");
        
        SizeAdapter adapter = new SizeAdapter(sizeList, currentSize, size -> {
            selectedSize = size;
            if (listener != null) {
                listener.onSizeSelected(selectedSize);
                dismiss();
            }
        });

        rvSizes.setLayoutManager(new GridLayoutManager(getContext(), 3));
        rvSizes.setAdapter(adapter);

        // If something was already selected (e.g. currentSize), set selectedSize
        if (currentSize != null && !currentSize.isEmpty() && !currentSize.equals("Select size")) {
            selectedSize = currentSize;
        }
    }
}
