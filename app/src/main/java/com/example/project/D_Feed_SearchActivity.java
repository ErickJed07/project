package com.example.project;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class D_Feed_SearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private RecyclerView recyclerView;
    private I_Profile_AddModels.UserAdapter userAdapter;
    private List<I_Profile_AddModels.User> allUsers;
    private TextView recentHeader;
    private TextView clearHistory;
    private SharedPreferences prefs;
    private static final String PREF_NAME = "RecentSearches";
    private static final String KEY_RECENT = "recent_uids";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.d4_feed_search_bar);

        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        searchEditText = findViewById(R.id.search_edit_text);
        recyclerView = findViewById(R.id.recent_recycler_view);
        ImageButton backButton = findViewById(R.id.back_button);
        clearHistory = findViewById(R.id.clear_history);
        recentHeader = findViewById(R.id.tv_search_header);

        backButton.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        allUsers = new ArrayList<>();
        userAdapter = new I_Profile_AddModels.UserAdapter(this, new ArrayList<>()) {
            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                I_Profile_AddModels.User user = mUsers.get(position);
                
                // Override click to add to recent
                View.OnClickListener originalClick = v -> {
                    addToRecent(user.getId());
                    if (user.getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                        startActivity(new Intent(D_Feed_SearchActivity.this, I_ProfileActivity.class));
                    } else {
                        Intent intent = new Intent(D_Feed_SearchActivity.this, I_UserProfileActivity.class);
                        intent.putExtra(I_UserProfileActivity.EXTRA_USER_ID, user.getId());
                        startActivity(intent);
                    }
                };
                
                holder.itemView.setOnClickListener(originalClick);
                holder.username.setOnClickListener(originalClick);
                holder.image_profile.setOnClickListener(originalClick);
            }
        };
        recyclerView.setAdapter(userAdapter);

        loadAllUsers();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        if (clearHistory != null) {
            clearHistory.setOnClickListener(v -> {
                prefs.edit().remove(KEY_RECENT).apply();
                filter("");
            });
        }
    }

    private void loadAllUsers() {
        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allUsers.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    I_Profile_AddModels.User user = ds.getValue(I_Profile_AddModels.User.class);
                    if (user != null) {
                        user.setId(ds.getKey());
                        if (ds.child("profilePhoto").exists()) {
                            user.setProfileImageUrl(ds.child("profilePhoto").getValue(String.class));
                        }
                        if (!user.getId().equals(myUid)) {
                            allUsers.add(user);
                        }
                    }
                }
                filter(searchEditText.getText().toString());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filter(String query) {
        List<I_Profile_AddModels.User> filtered = new ArrayList<>();
        if (query.isEmpty()) {
            List<String> recentIds = getRecentUids();
            if (!recentIds.isEmpty()) {
                recentHeader.setText("Recent Searches");
                clearHistory.setVisibility(View.VISIBLE);
                for (String id : recentIds) {
                    for (I_Profile_AddModels.User u : allUsers) {
                        if (u.getId().equals(id)) {
                            filtered.add(u);
                            break;
                        }
                    }
                }
            } else {
                recentHeader.setText("Suggested for You");
                clearHistory.setVisibility(View.GONE);
                // Show first 10 users as suggestions
                for (int i = 0; i < Math.min(10, allUsers.size()); i++) {
                    filtered.add(allUsers.get(i));
                }
            }
        } else {
            recentHeader.setText("Find Users");
            clearHistory.setVisibility(View.GONE);
            String q = query.toLowerCase().trim();
            for (I_Profile_AddModels.User u : allUsers) {
                if (u.getUsername() != null && u.getUsername().toLowerCase().contains(q)) {
                    filtered.add(u);
                }
            }
        }
        userAdapter.setFilteredList(filtered);
    }

    private List<String> getRecentUids() {
        String saved = prefs.getString(KEY_RECENT, "");
        if (saved.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(saved.split(",")));
    }

    public void addToRecent(String uid) {
        List<String> current = getRecentUids();
        current.remove(uid);
        current.add(0, uid);
        if (current.size() > 5) current = current.subList(0, 5);
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < current.size(); i++) {
            sb.append(current.get(i));
            if (i < current.size() - 1) sb.append(",");
        }
        prefs.edit().putString(KEY_RECENT, sb.toString()).apply();
    }
}
