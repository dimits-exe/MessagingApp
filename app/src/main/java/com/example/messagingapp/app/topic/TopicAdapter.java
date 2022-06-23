package com.example.messagingapp.app.topic;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;
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
import com.example.messagingapp.app.util.strategies.IErrorMessageStrategy;
import com.example.messagingapp.eventDeliverySystem.datastructures.Post;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter for the TopicActivity which handles how the contents of a conversation are laid out on
 * the screen and is responsible for creating the appropriate ViewHolders for the different types of
 * messages.
 *
 * @author Alex Mandelias
 */
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
    private static final String TAG = TopicAdapter.class.getName();

    private final IErrorMessageStrategy errorMessageStrategy;
    private final ITopicView topicView;
    private final TopicPresenter presenter;
    private List<Post> currentPosts;

    public TopicAdapter(TopicPresenter presenter, ITopicView topicView, IErrorMessageStrategy errorMessageStrategy) {
        this.presenter = presenter;
        this.topicView = topicView;
        this.errorMessageStrategy = errorMessageStrategy;
        currentPosts = presenter.getProfilePosts();
        setHasStableIds(false);
    }

    /**
     * Updates the Posts of this Adapter.
     *
     * @param newPosts the new Posts
     */
    @SuppressLint("NotifyDataSetChanged")
    public void updatePosts(List<Post> newPosts) {
        currentPosts = newPosts;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (Type.fromCode(viewType)) {
            case TEXT:
                return TextTopicViewHolder.fromParent(parent);
            case IMAGE:
            case VIDEO:
                return ImageTopicViewHolder.fromParent(parent);
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

        final Post post = currentPosts.get(position);

        holder.linearLayout.setGravity(
                post.getPostInfo().getPosterName().equals(presenter.getProfileName())
                ? Gravity.END
                : Gravity.START);

        switch (getTypeForPosition(position)) {
            case TEXT: {
                TextTopicViewHolder vh = (TextTopicViewHolder) holder;
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
                ImageTopicViewHolder vh = (ImageTopicViewHolder) holder;

                // set thumbnail
                File temp = presenter.getNewTempFile(".mp4");

                try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(temp))){
                    outputStream.write(post.getData());
                    Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(temp.toString(), MediaStore.Images.Thumbnails.MINI_KIND);

                    vh.imageView.setImageBitmap(thumbnail);

                    // on click go to video player activity
                    vh.imageView.setOnClickListener(v -> topicView.playVideo(temp));
                } catch (IOException e) {
                    Log.e(TAG, "Render video thumbnail", e);
                    errorMessageStrategy.showError("Unable to display video");
                }
                break;
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


    /**
     * ViewHolder superclass for all messages. All ViewHolders for the different message types must
     * extend this class.
     *
     * @author Alex Mandelias
     */
    static class TopicViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout linearLayout;

        public TopicViewHolder(@NonNull View itemView, LinearLayout linearLayout) {
            super(itemView);
            this.linearLayout = linearLayout;
        }
    }

    /**
     * ViewHolder for messages containing Text.
     *
     * @author Alex Mandelias
     */
    private static class TextTopicViewHolder extends TopicViewHolder {

        static TextTopicViewHolder fromParent(@NonNull ViewGroup parent) {
            return new TextTopicViewHolder(LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.message_text, parent, false));
        }

        private final TextView textView;

        public TextTopicViewHolder(View view) {
            super(view, view.findViewById(R.id.message_text_linearlayout));
            textView = view.findViewById(R.id.message_text_text);
        }
    }

    /**
     * ViewHolder for messages containing Image.
     *
     * @author Alex Mandelias
     */
    private static class ImageTopicViewHolder extends TopicViewHolder {

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

    // :sunflower:

    private Type getTypeForPosition(int position) {
        String extension = currentPosts.get(position).getPostInfo().getFileExtension();

        if (extension.equals("~txt"))
            return Type.TEXT;

        if (imageExtensions.contains(extension))
            return Type.IMAGE;

        if (videoExtensions.contains(extension))
            return Type.VIDEO;

        throw new RuntimeException("Missing Type for extension " + extension
                + ". Please add the extension to its corresponding list in the TopicAdapter private static Lists.");
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
