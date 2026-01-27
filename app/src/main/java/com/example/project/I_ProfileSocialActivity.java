package com.example.project;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
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

public class I_ProfileSocialActivity extends AppCompatActivity {

    private TextView usernameTextView;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private SearchView searchBar;
    private String profileid;
    private String firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i_profile_social_list);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            firebaseUser = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        profileid = getIntent().getStringExtra("profileid");
        if (profileid == null) profileid = firebaseUser;

        usernameTextView = findViewById(R.id.usernamemodelfan);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        searchBar = findViewById(R.id.search_bar);

        findViewById(R.id.backbutton).setOnClickListener(v -> finish());
        getUserInfo();

        ViewPagerAdapter adapter = new ViewPagerAdapter(this, profileid);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Models" : "Fans");
        }).attach();

        getCounts();

        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    if (fragment instanceof I_ProfileSocialFragment && fragment.isAdded()) {
                        ((I_ProfileSocialFragment) fragment).filterList(newText);
                    }
                }
                return true;
            }
        });

        viewPager.setCurrentItem("fans".equals(getIntent().getStringExtra("title")) ? 1 : 0, false);
    }

    private void getUserInfo() {
        FirebaseDatabase.getInstance().getReference("Users").child(profileid).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) usernameTextView.setText(snapshot.child("username").getValue(String.class));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void getCounts() {
        FirebaseDatabase.getInstance().getReference("Users").child(profileid).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long models = getCountFrom(snapshot, "Models", "ModelsList");
                    long fans = getCountFrom(snapshot, "Fans", "FansList");
                    if (tabLayout.getTabCount() >= 2) {
                        if (tabLayout.getTabAt(0) != null) tabLayout.getTabAt(0).setText("Models (" + models + ")");
                        if (tabLayout.getTabAt(1) != null) tabLayout.getTabAt(1).setText("Fans (" + fans + ")");
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private long getCountFrom(DataSnapshot snapshot, String keyInt, String keyList) {
        if (snapshot.hasChild(keyInt)) {
            Object val = snapshot.child(keyInt).getValue();
            return val instanceof Long ? (Long) val : Long.parseLong(val.toString());
        }
        return snapshot.hasChild(keyList) ? snapshot.child(keyList).getChildrenCount() : 0;
    }

    static class ViewPagerAdapter extends FragmentStateAdapter {
        private final String profileId;
        public ViewPagerAdapter(@NonNull FragmentActivity fa, String profileId) { super(fa); this.profileId = profileId; }
        @NonNull @Override public Fragment createFragment(int pos) {
            return I_ProfileSocialFragment.newInstance(profileId, pos == 0 ? "Models" : "Fans");
        }
        @Override public int getItemCount() { return 2; }
    }
}
