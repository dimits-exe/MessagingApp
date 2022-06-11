package com.example.messagingapp.app.topic;

import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messagingapp.app.R;
import com.example.messagingapp.eventDeliverySystem.datastructures.Post;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TopicAdapter extends RecyclerView.Adapter<TopicAdapter.TopicViewHolder> {

    private static final Set<String> imageExtensions = new HashSet<String>() {{
        add("jpeg");
        add("jpg");
        add("png");
        add("tiff");
        // ...
    }};

    private static final Set<String> videoExtensions = new HashSet<String>() {{
        add("mp4");
        add("wav");
        // ...
    }};

    private final TopicPresenter presenter;
    private List<Post> currentPosts;

    public TopicAdapter(TopicPresenter presenter) {
        this.presenter = presenter;
        currentPosts = presenter.getProfilePosts();
        setHasStableIds(false);
    }

    public void updatePosts(List<Post> newPosts) {
        currentPosts = newPosts;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (Type.fromCode(viewType)) {
            case TEXT:
                return PlainTextTopicViewHolder.fromParent(parent);
            case IMAGE:
                return ImageTopicViewHolder.fromParent(parent);
            case VIDEO:
                throw new RuntimeException("NOT YET IMPLEMENTED");
            default:
                throw new RuntimeException("No Type associated with viewType " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        List<Post> freshPosts = presenter.getProfilePosts();
        if (!currentPosts.equals(freshPosts)) {
            updatePosts(freshPosts);
        }

        Post post = currentPosts.get(position);

        holder.linearLayout.setGravity(
                post.getPostInfo().getPosterName().equals(presenter.getProfileName())
                ? Gravity.END
                : Gravity.START);

        switch (getTypeForPosition(position)) {
            case TEXT: {
                PlainTextTopicViewHolder vh = (PlainTextTopicViewHolder) holder;
                vh.textView.setText(new String(post.getData()));
                break;
            }
            case IMAGE: {
                ImageTopicViewHolder vh = (ImageTopicViewHolder) holder;
                byte[] data = post.getData();
                vh.imageView.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
                break;
            }
            case VIDEO: {
                throw new RuntimeException("NOT YET IMPLEMENTED");
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return getTypeForPosition(position).code;
    }

    @Override
    public int getItemCount() {
        return currentPosts.size();
    }

    static class TopicViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout linearLayout;

        public TopicViewHolder(@NonNull View itemView, LinearLayout linearLayout) {
            super(itemView);
            this.linearLayout = linearLayout;
        }
    }

    static class PlainTextTopicViewHolder extends TopicViewHolder {

        static PlainTextTopicViewHolder fromParent(@NonNull ViewGroup parent) {
            return new PlainTextTopicViewHolder(LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.message_text, parent, false));
        }

        private final TextView textView;

        public PlainTextTopicViewHolder(View view) {
            super(view, view.findViewById(R.id.message_text_linearlayout));
            textView = view.findViewById(R.id.message_text_text);
        }
    }

    static class ImageTopicViewHolder extends TopicViewHolder {

        static ImageTopicViewHolder fromParent(@NonNull ViewGroup parent) {
            return new ImageTopicViewHolder(LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.message_image, parent, false));
        }

        private final ImageView imageView;

        public ImageTopicViewHolder(View view) {
            super(view, view.findViewById(R.id.message_image_linearlayout));
            imageView = view.findViewById(R.id.message_image_imageview);
        }
    }

    private Type getTypeForPosition(int position) {
        String extension = currentPosts.get(position).getPostInfo().getFileExtension();

        if (extension.equals("~txt"))
            return Type.TEXT;

        if (imageExtensions.contains(extension))
            return Type.IMAGE;

        if (videoExtensions.contains(extension))
            return Type.VIDEO;

        throw new RuntimeException("Missing Type for extension " + extension
                + ". Please add the extension to the correct list in the TopicAdapter private static Lists.");
    }

    private enum Type {
        TEXT(0), IMAGE(1), VIDEO(2);

        private final int code;

        public static Type fromCode(int code) {
            for (Type type : Type.values())
                if (type.code == code)
                    return type;

            throw new RuntimeException("Missing Type for code " + code);
        }

        Type(int code) {
            this.code = code;
        }
    }
}
