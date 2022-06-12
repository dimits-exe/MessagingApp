package com.example.messagingapp.app.topiclist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messagingapp.app.R;
import com.example.messagingapp.eventDeliverySystem.filesystem.Profile;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter for the TopicListActivity which handles how the conversations are laid out on the screen
 * and is responsible for transitioning to the corresponding TopicActivity when the user clicks on a
 * conversation.
 *
 * @author Alex Mandelias
 */
class TopicListAdapter extends RecyclerView.Adapter<TopicListAdapter.TopicPreviewViewHolder> {

    private final MyClickListener myClickListener;
    private final Profile profile;
    private final List<String> topicNames;

    public TopicListAdapter(Profile profile, MyClickListener myClickListener) {
        this.myClickListener = myClickListener;
        this.profile = profile;
        topicNames = profile
                .getTopics()
                .keySet()
                .stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    @NonNull
    @Override
    public TopicPreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.topic_preview, parent, false);

        return new TopicPreviewViewHolder(view, myClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicPreviewViewHolder holder, int position) {
        String topicName = topicNames.get(position);

        holder.topicName.setText(topicName);
        holder.unreadCount.setText(String.valueOf(profile.getUnread(topicNames.get(position))));
    }

    @Override
    public int getItemCount() {
        return topicNames.size();
    }

    static class TopicPreviewViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView topicName, unreadCount;
        private final MyClickListener myClickListener;

        public TopicPreviewViewHolder(@NonNull View topicPreview, MyClickListener myClickListener) {
            super(topicPreview);
            this.myClickListener = myClickListener;

            topicName = topicPreview.findViewById(R.id.topicpreview_topic_name);
            unreadCount = topicPreview.findViewById(R.id.topicpreview_unread_count);

            topicName.setOnClickListener(this);
            unreadCount.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            myClickListener.onClick(topicName.getText().toString());
        }
    }
}
