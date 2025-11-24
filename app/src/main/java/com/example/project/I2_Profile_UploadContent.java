package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Import Button class

import androidx.fragment.app.Fragment;

public class I2_Profile_UploadContent extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.i2_upload_content, container, false);

        // Find the upload button by its ID
        Button uploadButton = rootView.findViewById(R.id.upload_button);

        // Set up the button click listener to navigate to the upload activity
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start the Upload Activity
                Intent intent = new Intent(getActivity(), I_Post_UploadActivity.class);
                startActivity(intent);
            }
        });

        return rootView;
    }
}
