package com.example.imageblog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private final List<Post> posts;

    public ImageAdapter(List<Post> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.textAuthor.setText(post.getAuthor() == null || post.getAuthor().isEmpty() ? "Unknown" : post.getAuthor());
        holder.textTitle.setText(post.getTitle() == null ? "" : post.getTitle());
        holder.textBody.setText(post.getText() == null ? "" : post.getText());
        holder.textDate.setText(post.getPublishedDate() == null ? "" : post.getPublishedDate());

        String url = post.getImageUrl();
        if (url != null && !url.isEmpty()) {
            Glide.with(holder.imageViewItem.getContext())
                    .load(url)
                    .centerCrop()
                    .into(holder.imageViewItem);
        } else {
            // 이미지가 없으면 뷰를 비우거나 기본 이미지 설정
            holder.imageViewItem.setImageDrawable(null);
        }
    }

    @Override
    public int getItemCount() {
        return posts == null ? 0 : posts.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewItem;
        TextView textAuthor;
        TextView textTitle;
        TextView textDate;
        TextView textBody;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageViewItem = itemView.findViewById(R.id.imageViewItem);
            textAuthor = itemView.findViewById(R.id.textAuthor);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDate = itemView.findViewById(R.id.textDate);
            textBody = itemView.findViewById(R.id.textBody);
        }
    }
}
