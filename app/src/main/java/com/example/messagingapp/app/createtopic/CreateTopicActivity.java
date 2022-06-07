package com.example.messagingapp.app.createtopic;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.example.messagingapp.app.R;
import com.example.messagingapp.app.topiclist.TopicListActivity;
import com.example.messagingapp.app.util.ErrorDialogFragment;
import com.example.messagingapp.eventDeliverySystem.User;
import com.example.messagingapp.eventDeliverySystem.filesystem.FileSystemException;
import com.example.messagingapp.eventDeliverySystem.server.ServerException;

public class CreateTopicActivity extends AppCompatActivity {

    private User user;

    private EditText topicNameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_topic);

        user = (User) getIntent().getExtras().getSerializable(TopicListActivity.USER);
        topicNameEditText = findViewById(R.id.create_topicname);
        findViewById(R.id.create_submit_button).setOnClickListener(e -> onSubmit());
        // findViewById().setOnClickListener(e -> finish()); // TODO: add back button
    }

    private void onSubmit() {

        String topicName = topicNameEditText.getText().toString();

        try {
            final boolean success = user.createTopic(topicName);
            if (!success)
                topicNameEditText.setError("Topic with name " + topicName + " already exists");
            else {
                topicNameEditText.setError(null);
                Toast.makeText(getApplicationContext(),
                        "Topic " + topicName + " created successfully",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        } catch (ServerException | FileSystemException e) {
            closeWithFatalError(e);
        }
    }

    private void closeWithFatalError(Exception e) {
        ErrorDialogFragment.startFromException(getFragmentManager(), e);
    }
}