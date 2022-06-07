package com.example.messagingapp.app.topiclist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messagingapp.app.R;
import com.example.messagingapp.app.createtopic.CreateTopicActivity;
import com.example.messagingapp.eventDeliverySystem.User;

public class TopicListActivity extends AppCompatActivity {
    public static final String ARG_USER = "USER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);

        Intent old = getIntent();
        User user = (User) old.getSerializableExtra(ARG_USER);


        ((TextView) findViewById(R.id.topiclist_username)).setText(user.getCurrentProfile().getName());

        findViewById(R.id.topiclist_add_button).setOnClickListener(e -> {
            Intent intent = new Intent(this, CreateTopicActivity.class);
            intent.putExtra(ARG_USER, user);
            startActivity(intent);
        });

        RecyclerView recyclerView = findViewById(R.id.topiclist_recycler_view);
        recyclerView.setAdapter(new TopicPreviewAdapter(user.getCurrentProfile()));
        recyclerView.setHasFixedSize(true);
    }

}
