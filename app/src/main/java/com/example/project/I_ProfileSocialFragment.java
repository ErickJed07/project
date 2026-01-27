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

public class I_ProfileSocialFragment extends Fragment {

    private String profileId;
    private String type;
    private RecyclerView recyclerView;
    private I_ProfileSocialAdapter userAdapter;
    private List<String> userList;
    private List<String> allProfileUserIds;
    private ValueEventListener currentListener;
    private DatabaseReference currentReference;

    public I_ProfileSocialFragment() { }

    public static I_ProfileSocialFragment newInstance(String profileId, String type) {
        I_ProfileSocialFragment fragment = new I_ProfileSocialFragment();
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
        rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.recyclerView = rv;
        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        userList = new ArrayList<>();
        allProfileUserIds = new ArrayList<>();
        userAdapter = new I_ProfileSocialAdapter(requireContext(), userList);
        recyclerView.setAdapter(userAdapter);

        startListeningForData("Models".equals(type) ? "ModelsList" : "FansList");
        return rv;
    }

    public void filterList(String text) {
        String queryText = (text != null) ? text.trim().toLowerCase() : "";
        if (queryText.isEmpty()) {
            userList.clear();
            userList.addAll(allProfileUserIds);
            userAdapter.notifyDataSetChanged();
        } else {
            searchUsers(queryText);
        }
    }

    private void startListeningForData(String nodeName) {
        if (currentListener != null && currentReference != null) currentReference.removeEventListener(currentListener);
        currentReference = FirebaseDatabase.getInstance().getReference("Users").child(profileId).child(nodeName);
        currentListener = currentReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                allProfileUserIds.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.getKey();
                    userList.add(id);
                    allProfileUserIds.add(id);
                }
                userAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void searchUsers(String s) {
        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("username").startAt(s).endAt(s + "\uf8ff");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    if (allProfileUserIds.contains(ds.getKey())) userList.add(ds.getKey());
                }
                userAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }
}
