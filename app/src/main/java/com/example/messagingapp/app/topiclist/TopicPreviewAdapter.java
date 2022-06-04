package com.example.messagingapp.app.topiclist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messagingapp.app.R;
import com.example.messagingapp.eventDeliverySystem.datastructures.Topic;
import com.example.messagingapp.eventDeliverySystem.filesystem.Profile;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class TopicPreviewAdapter extends RecyclerView.Adapter<TopicPreviewAdapter.TopicPreviewViewHolder> {

    private final Profile profile;
    private final List<String> topicNames;

    public TopicPreviewAdapter(Profile profile) {
        this.profile = profile;
        topicNames = profile.getTopics()
                .stream()
                .map(Topic::getName)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    @NonNull
    @Override
    public TopicPreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.topic_preview, parent, false);

        return new TopicPreviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicPreviewViewHolder holder, int position) {
        holder.topicName.setText(topicNames.get(position));
        holder.unreadCount.setText(String.valueOf(profile.getUnread(topicNames.get(position))));
    }

    @Override
    public int getItemCount() {
        return topicNames.size();
    }

    static class TopicPreviewViewHolder extends RecyclerView.ViewHolder {

        private final TextView topicName, unreadCount;

        public TopicPreviewViewHolder(@NonNull View topicPreview) {
            super(topicPreview);

            topicName = topicPreview.findViewById(R.id.topicpreview_topic_name);
            unreadCount = topicPreview.findViewById(R.id.topicpreview_unread_count);
        }
    }
}
