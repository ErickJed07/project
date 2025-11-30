package com.example.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class I_ProfileModelFan_List2 extends Fragment {

    private String profileId;
    private String type;

    private RecyclerView recyclerView;
    private I_ProfileModelFan_Adapter userAdapter;

    private List<String> userList;          // The list currently shown on screen
    private List<String> allProfileUserIds; // A backup list of ALL IDs for this page (for filtering)

    // We keep track of the listener to remove it when searching to avoid conflicts
    private ValueEventListener currentListener;
    private DatabaseReference currentReference;

    public I_ProfileModelFan_List2() { }

    public static I_ProfileModelFan_List2 newInstance(String profileId, String type) {
        I_ProfileModelFan_List2 fragment = new I_ProfileModelFan_List2();
        Bundle args = new Bundle();
        args.putString("profileId", profileId);
        args.putString("type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            profileId = getArguments().getString("profileId");
            type = getArguments().getString("type");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RecyclerView rv = new RecyclerView(requireContext());
        rv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rv.setId(View.generateViewId());

        this.recyclerView = rv;
        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        userList = new ArrayList<>();
        allProfileUserIds = new ArrayList<>(); // Init backup list
        userAdapter = new I_ProfileModelFan_Adapter(requireContext(), userList);
        recyclerView.setAdapter(userAdapter);

        // Initial Load
        String nodeName = "Models".equals(type) ? "ModelsList" : "FansList";
        startListeningForData(nodeName);

        return rv;
    }

    // Called by Activity when typing
    public void filterList(String text) {
        // Convert to lowercase once, check for empty or null
        String queryText = (text != null) ? text.trim().toLowerCase() : "";

        if (queryText.isEmpty()) {
            // Text is empty? Show the full list we already loaded
            restoreFullList();
        } else {
            // Text exists? Search Firebase
            searchUsers(queryText);
        }
    }

    private void restoreFullList() {
        userList.clear();
        // If we have cached IDs, just show them immediately
        if (allProfileUserIds != null && !allProfileUserIds.isEmpty()) {
            userList.addAll(allProfileUserIds);
        } else {
            // If for some reason the backup is empty, ensure the listener is running
            String nodeName = "Models".equals(type) ? "ModelsList" : "FansList";
            // Only restart if we suspect the listener isn't providing data
            if (currentListener == null) {
                startListeningForData(nodeName);
            }
        }
        userAdapter.notifyDataSetChanged();
    }


    private void startListeningForData(String nodeName) {
        // Remove old listener if exists
        if (currentListener != null && currentReference != null) {
            currentReference.removeEventListener(currentListener);
        }

        currentReference = FirebaseDatabase.getInstance().getReference("Users")
                .child(profileId).child(nodeName);

        currentListener = currentReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                allProfileUserIds.clear(); // Update backup list

                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    String id = snapshot1.getKey();
                    userList.add(id);
                    allProfileUserIds.add(id);
                }
                userAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void searchUsers(String s) {
        // We don't want the main list listener overwriting our search results
        // But we don't remove it entirely because we want it back when search is cleared.
        // Since we use a separate 'userList', we just clear that.

        Query query = FirebaseDatabase.getInstance().getReference("Users")
                .orderByChild("username")
                .startAt(s)
                .endAt(s + "\uf8ff");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String foundUserId = snapshot.getKey();

                    // FAST CHECK: Is this found user actually in our list?
                    // We check against 'allProfileUserIds' which we loaded at start
                    if (allProfileUserIds.contains(foundUserId)) {
                        userList.add(foundUserId);
                    }
                }
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }
}
