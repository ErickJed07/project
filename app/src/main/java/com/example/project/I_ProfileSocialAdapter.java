package com.example.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;

public class I_ProfileSocialAdapter extends RecyclerView.Adapter<I_ProfileSocialAdapter.ViewHolder> {

    private Context mContext;
    private List<String> mUsers;
    private FirebaseUser firebaseUser;

    public I_ProfileSocialAdapter(Context mContext, List<String> mUsers) {
        this.mContext = mContext;
        this.mUsers = mUsers;
        this.firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.i_profile_modelfan_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final String userid = mUsers.get(position);
        holder.btn_follow.setVisibility(userid.equals(firebaseUser.getUid()) ? View.GONE : View.VISIBLE);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    holder.username.setText(snapshot.child("username").getValue(String.class));
                    String profilePhotoUrl = snapshot.child("profilePhoto").getValue(String.class);
                    Glide.with(mContext)
                            .load(profilePhotoUrl != null && !profilePhotoUrl.isEmpty() && !profilePhotoUrl.equals("default") ? profilePhotoUrl : R.drawable.ic_placeholder_2)
                            .circleCrop()
                            .placeholder(R.drawable.ic_placeholder_2)
                            .into(holder.img_profile);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });

        isFollowing(userid, holder.btn_follow);

        holder.btn_follow.setOnClickListener(v -> {
            DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("Users");
            if (holder.btn_follow.getText().toString().equalsIgnoreCase("Follow Model")) {
                db.child(firebaseUser.getUid()).child("ModelsList").child(userid).setValue(true);
                db.child(userid).child("FansList").child(firebaseUser.getUid()).setValue(true);
                updateCount(firebaseUser.getUid(), "Models", 1);
                updateCount(userid, "Fans", 1);
            } else {
                db.child(firebaseUser.getUid()).child("ModelsList").child(userid).removeValue();
                db.child(userid).child("FansList").child(firebaseUser.getUid()).removeValue();
                updateCount(firebaseUser.getUid(), "Models", -1);
                updateCount(userid, "Fans", -1);
            }
        });
    }

    @Override
    public int getItemCount() { return mUsers.size(); }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView username;
        public Button btn_follow;
        public ImageView img_profile;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            btn_follow = itemView.findViewById(R.id.btn_follow);
            img_profile = itemView.findViewById(R.id.img_profile);
        }
    }

    private void isFollowing(String userid, Button button) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUser.getUid()).child("ModelsList");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                button.setText(snapshot.child(userid).exists() ? "Following" : "Follow Model");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void updateCount(String userId, String field, int increment) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(userId).child(field);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long currentCount = 0;
                if (snapshot.exists()) {
                    try { currentCount = Long.parseLong(snapshot.getValue().toString()); } catch (Exception e) {}
                }
                ref.setValue(Math.max(0, currentCount + increment));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
}
