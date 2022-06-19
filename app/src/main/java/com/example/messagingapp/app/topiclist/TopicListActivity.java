package com.example.messagingapp.app.topiclist;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messagingapp.app.R;
import com.example.messagingapp.app.createtopic.CreateTopicActivity;
import com.example.messagingapp.app.topic.TopicActivity;
import com.example.messagingapp.app.util.LoggedInUserHolder;
import com.example.messagingapp.eventDeliverySystem.IUser;
import com.example.messagingapp.eventDeliverySystem.User;

import java.util.Objects;

public class TopicListActivity extends AppCompatActivity {
    public static final String ARG_USER = "USER";

    private IUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);

        user = LoggedInUserHolder.getInstance();

        ((TextView) findViewById(R.id.topiclist_username)).setText(user.getCurrentProfile().getName());

        findViewById(R.id.topiclist_add_button).setOnClickListener(e -> {
            Intent intent = new Intent(this, CreateTopicActivity.class);
            startActivity(intent);
        });

        RecyclerView recyclerView = findViewById(R.id.topiclist_recycler_view);

        TopicListAdapter adapter = new TopicListAdapter(user.getCurrentProfile(), topicName -> {
            Log.e("TLA", "switching to showing " + topicName);
            Intent intent = new Intent(this, TopicActivity.class);
            intent.putExtra(TopicActivity.ARG_TOPIC_NAME, topicName);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
    }

    @SuppressLint("NotifyDataSetChanged") // cry about it
    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Sex", getIntent().toString());
        user = (User) getIntent().getSerializableExtra(ARG_USER);
        Objects.requireNonNull(((RecyclerView) findViewById(R.id.topiclist_recycler_view))
                .getAdapter()).notifyDataSetChanged();
    }

}
