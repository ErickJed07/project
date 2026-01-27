package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class I_ProfileContentFragment extends Fragment {

    private static final String ARG_TYPE = "CONTENT_TYPE";
    public static final String TYPE_UPLOAD = "UPLOAD";
    public static final String TYPE_LIKED = "LIKED";
    public static final String TYPE_FAVORITE = "FAVORITE";

    private String contentType;
    private RecyclerView recyclerView;
    private I_ProfileGridAdapter adapter;
    private List<I_PostEvent> postList = new ArrayList<>();
    private FirebaseUser currentUser;

    public static I_ProfileContentFragment newInstance(String type) {
        I_ProfileContentFragment fragment = new I_ProfileContentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            contentType = getArguments().getString(ARG_TYPE);
        }
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.i_fav_content, container, false);
        recyclerView = view.findViewById(R.id.favcontentRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        boolean showAdd = TYPE_UPLOAD.equals(contentType);
        adapter = new I_ProfileGridAdapter(getContext(), postList, showAdd, new I_ProfileGridAdapter.OnItemClickListener() {
            @Override
            public void onAddClick() {
                startActivity(new Intent(getActivity(), I_PostUploadActivity.class));
            }

            @Override
            public void onImageClick(I_PostEvent post, int position) {
                Intent intent = new Intent(getContext(), I_ProfilePostActivity.class);
                intent.putExtra(I_ProfilePostActivity.EXTRA_TYPE, contentType);
                intent.putExtra(I_ProfilePostActivity.EXTRA_POST_ID, post.getPostId());
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(adapter);
        fetchData();
        return view;
    }

    private void fetchData() {
        if (currentUser == null) return;
        String uid = currentUser.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PostEvents");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    I_PostEvent post = ds.getValue(I_PostEvent.class);
                    if (post == null) continue;
                    post.setPostId(ds.getKey());

                    boolean isCreator = post.getUserId() != null && post.getUserId().equals(uid);
                    boolean shouldAdd = false;

                    switch (contentType) {
                        case TYPE_UPLOAD:
                            shouldAdd = isCreator;
                            break;
                        case TYPE_LIKED:
                            Map<String, Boolean> likes = post.getHeartLiked();
                            shouldAdd = (likes != null && Boolean.TRUE.equals(likes.get(uid))) && !isCreator;
                            break;
                        case TYPE_FAVORITE:
                            Map<String, Boolean> favs = post.getFavList();
                            shouldAdd = (favs != null && Boolean.TRUE.equals(favs.get(uid))) && !isCreator;
                            break;
                    }

                    if (shouldAdd && post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
                        postList.add(post);
                    }
                }
                Collections.reverse(postList);
                if (adapter != null) adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
