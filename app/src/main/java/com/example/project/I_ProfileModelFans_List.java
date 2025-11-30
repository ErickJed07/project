package com.example.project;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView; // Import this
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class I_ProfileModelFans_List extends AppCompatActivity {

    private TextView usernameTextView;
    private ImageButton backButton;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private SearchView searchBar; // CHANGED: Explicitly typed as SearchView

    private String profileid;
    private String firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i_profile_modelfanlist);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            firebaseUser = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        profileid = getIntent().getStringExtra("profileid");
        if (profileid == null) profileid = firebaseUser;

        // Initialize UI
        usernameTextView = findViewById(R.id.usernamemodelfan);
        backButton = findViewById(R.id.backbutton);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        searchBar = findViewById(R.id.search_bar); // Matches XML ID

        backButton.setOnClickListener(v -> finish());

        getUserInfo();

        // Setup ViewPager
        ViewPagerAdapter adapter = new ViewPagerAdapter(this, profileid);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Models" : "Fans");
        }).attach();

        getCounts();

        // --- FIXED: Search Listener for SearchView ---
        // SearchView uses OnQueryTextListener, not TextWatcher
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Pass the text to the active Fragments
                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    // Ensure we are checking for the correct Fragment class name (List2)
                    if (fragment instanceof I_ProfileModelFan_List2 && fragment.isAdded()) {
                        ((I_ProfileModelFan_List2) fragment).filterList(newText);
                    }
                }
                return true;
            }
        });
        // ----------------------------

        String title = getIntent().getStringExtra("title");
        if (title != null && title.equals("fans")) viewPager.setCurrentItem(1, false);
        else viewPager.setCurrentItem(0, false);
    }

    // ... (Keep getUserInfo, getCounts, getCountFromSnapshot as they were) ...
    private void getUserInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(profileid);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    if (usernameTextView != null) usernameTextView.setText(username);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void getCounts() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(profileid);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long modelsCount = getCountFromSnapshot(snapshot, "Models", "ModelsList");
                    long fansCount = getCountFromSnapshot(snapshot, "Fans", "FansList");
                    if (tabLayout.getTabCount() >= 2) {
                        if (tabLayout.getTabAt(0) != null) tabLayout.getTabAt(0).setText("Models (" + modelsCount + ")");
                        if (tabLayout.getTabAt(1) != null) tabLayout.getTabAt(1).setText("Fans (" + fansCount + ")");
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private long getCountFromSnapshot(DataSnapshot snapshot, String keyInt, String keyList) {
        long count = 0;
        if (snapshot.hasChild(keyInt)) {
            Object val = snapshot.child(keyInt).getValue();
            if (val instanceof Long) count = (Long) val;
            else if (val instanceof String) count = Long.parseLong((String) val);
        } else if (snapshot.hasChild(keyList)) {
            count = snapshot.child(keyList).getChildrenCount();
        }
        return count;
    }

    static class ViewPagerAdapter extends FragmentStateAdapter {
        private final String profileId;
        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, String profileId) {
            super(fragmentActivity);
            this.profileId = profileId;
        }
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Ensure you instantiate the correct Fragment Class
            return I_ProfileModelFan_List2.newInstance(profileId, position == 0 ? "Models" : "Fans");
        }
        @Override
        public int getItemCount() { return 2; }
    }
}
