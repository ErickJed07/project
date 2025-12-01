package com.example.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class I_ProfileModelFan_Adapter extends RecyclerView.Adapter<I_ProfileModelFan_Adapter.ViewHolder> {

    private Context mContext;
    private List<String> mUsers;
    private FirebaseUser firebaseUser;

    public I_ProfileModelFan_Adapter(Context mContext, List<String> mUsers) {
        this.mContext = mContext;
        this.mUsers = mUsers;
        this.firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Make sure you have a layout file named 'user_item.xml'
        View view = LayoutInflater.from(mContext).inflate(R.layout.i_profile_modelfan_user, parent, false);
        return new ViewHolder(view);
    }

// 2. Update onBindViewHolder inside the Adapter class

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final String userid = mUsers.get(position);

        holder.btn_follow.setVisibility(View.VISIBLE);

        // Hide follow button if viewing yourself in the list
        if (userid.equals(firebaseUser.getUid())) {
            holder.btn_follow.setVisibility(View.GONE);
        }

        // 1. Load User Info (Username AND Profile Photo)
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Set Username
                    String username = snapshot.child("username").getValue(String.class);
                    holder.username.setText(username);

                    // --- NEW: Get Profile Photo ---
                    String profilePhotoUrl = null;
                    if (snapshot.child("profilePhoto").exists()) {
                        profilePhotoUrl = snapshot.child("profilePhoto").getValue(String.class);
                    }

                    // Load Image with Glide
                    if (profilePhotoUrl != null && !profilePhotoUrl.isEmpty() && !profilePhotoUrl.equals("default")) {
                        try {
                            Glide.with(mContext)
                                    .load(profilePhotoUrl)
                                    .placeholder(R.drawable.ic_placeholder_2)
                                    .error(R.drawable.ic_placeholder_2)
                                    .circleCrop()
                                    .into(holder.img_profile);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Load default image if URL is missing or "default"
                        Glide.with(mContext)
                                .load(R.drawable.ic_placeholder_2)
                                .circleCrop()
                                .into(holder.img_profile);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // 2. Check Following Status to update Button Text
        isFollowing(userid, holder.btn_follow);

        // 3. Handle Click Event (Follow/Unfollow)
        holder.btn_follow.setOnClickListener(v -> {
            if (holder.btn_follow.getText().toString().equalsIgnoreCase("Follow Model")) {
                // ... (Your existing Follow logic) ...
                FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUser.getUid())
                        .child("ModelsList").child(userid).setValue(true);
                FirebaseDatabase.getInstance().getReference().child("Users").child(userid)
                        .child("FansList").child(firebaseUser.getUid()).setValue(true);
                updateCount(firebaseUser.getUid(), "Models", 1);
                updateCount(userid, "Fans", 1);

            } else {
                // ... (Your existing Unfollow logic) ...
                FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUser.getUid())
                        .child("ModelsList").child(userid).removeValue();
                FirebaseDatabase.getInstance().getReference().child("Users").child(userid)
                        .child("FansList").child(firebaseUser.getUid()).removeValue();
                updateCount(firebaseUser.getUid(), "Models", -1);
                updateCount(userid, "Fans", -1);
            }
        });
    }


    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    // 3. Update the ViewHolder class at the bottom of the file

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView username;
        public Button btn_follow;
        public ImageView img_profile; // Add this line

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs exist in your user_item.xml (i_profile_modelfan_user.xml)
            username = itemView.findViewById(R.id.username);
            btn_follow = itemView.findViewById(R.id.btn_follow);
            img_profile = itemView.findViewById(R.id.img_profile); // Add this line
        }
    }


    // Check if we are already following this user to set button text
    private void isFollowing(String userid, Button button) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child(firebaseUser.getUid()).child("ModelsList");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(userid).exists()) {
                    button.setText("Following");
                } else {
                    button.setText("Follow Model");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // Helper to safely increment/decrement integer counts in Firebase
    private void updateCount(String userId, String field, int increment) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(userId).child(field);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long currentCount = 0;
                if (snapshot.exists()) {
                    try {
                        currentCount = Long.parseLong(snapshot.getValue().toString());
                    } catch (Exception e) {
                        currentCount = 0;
                    }
                }
                // Don't allow negative numbers
                long newCount = Math.max(0, currentCount + increment);
                ref.setValue(newCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
}
