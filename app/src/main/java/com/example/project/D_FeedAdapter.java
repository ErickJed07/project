package com.example.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;

import java.util.List;

public class D_FeedAdapter extends RecyclerView.Adapter<D_FeedAdapter.PostViewHolder> {

    private Context context;
    private List<I_PostUpload_Event> postList;

    public D_FeedAdapter(Context context, List<I_PostUpload_Event> postList) {
        this.context = context;
        this.postList = postList;
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.d2_feed_item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PostViewHolder holder, int position) {
        I_PostUpload_Event postEvent = postList.get(position);

        // Set data on the views
        holder.userNameTextView.setText(postEvent.getUsername());
        holder.captionTextView.setText(postEvent.getCaption());

        // Check if there are images and set ViewPager2 adapter
        if (postEvent.getImageUrls() != null && !postEvent.getImageUrls().isEmpty()) {
            D_Feed_ImageViewAdapter imageAdapter = new D_Feed_ImageViewAdapter(context, postEvent.getImageUrls());
            holder.viewPager2.setAdapter(imageAdapter);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView, captionTextView;
        ViewPager2 viewPager2;  // ViewPager2 to display images

        public PostViewHolder(View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.userName_post);
            captionTextView = itemView.findViewById(R.id.caption_post);
            viewPager2 = itemView.findViewById(R.id.post_Pic);  // Find ViewPager2
        }
    }
}
