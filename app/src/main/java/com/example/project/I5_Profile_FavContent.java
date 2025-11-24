package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import android.widget.TextView;

public class I5_Profile_FavContent extends Fragment {

    public I5_Profile_FavContent() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.i4_fav_content, container, false);

        // You can add logic here to display the favorite content dynamically
        TextView favContentText = view.findViewById(R.id.favContentText);
        favContentText.setText("Your favorite content will appear here.");

        return view;
    }
}
