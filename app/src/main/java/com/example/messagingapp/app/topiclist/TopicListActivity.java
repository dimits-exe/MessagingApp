package com.example.messagingapp.app.topiclist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.messagingapp.app.ConnectActivity;
import com.example.messagingapp.app.LoginActivity;
import com.example.messagingapp.app.R;
import com.example.messagingapp.app.createtopic.CreateTopicActivity;
import com.example.messagingapp.app.util.ErrorDialogFragment;
import com.example.messagingapp.eventDeliverySystem.User;
import com.example.messagingapp.eventDeliverySystem.filesystem.FileSystemException;
import com.example.messagingapp.eventDeliverySystem.server.ServerException;

import java.net.UnknownHostException;

public class TopicListActivity extends AppCompatActivity {

    public static final String USER = "USER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);

        Intent old = getIntent();
        String serverIp = old.getStringExtra(ConnectActivity.SERVER_IP);
        int serverPort = old.getIntExtra(ConnectActivity.SERVER_PORT, -1);
        String username = old.getStringExtra(LoginActivity.USERNAME);
        boolean newUser = old.getBooleanExtra(LoginActivity.NEW_USER, false);

        User user;
        try {
            if (newUser)
                user = User.createNew(serverIp, serverPort, null /* TODO */, username);
            else
                user = User.loadExisting(serverIp, serverPort, null /* TODO */, username);
        } catch (ServerException | FileSystemException | UnknownHostException e) {
            ErrorDialogFragment.startFromException(getFragmentManager(), e);
            return;
        }

        ((TextView) findViewById(R.id.topiclist_username)).setText(user.getCurrentProfile().getName());

        findViewById(R.id.topiclist_add_button).setOnClickListener(e -> {
            Intent intent = new Intent(this, CreateTopicActivity.class);
            intent.putExtra(USER, user);
            startActivity(intent);
        });

        RecyclerView recyclerView = findViewById(R.id.topiclist_recycler_view);
        recyclerView.setAdapter(new TopicPreviewAdapter(user.getCurrentProfile()));
        recyclerView.setHasFixedSize(true);
    }
}
