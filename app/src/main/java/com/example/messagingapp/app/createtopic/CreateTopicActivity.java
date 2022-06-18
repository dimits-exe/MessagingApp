package com.example.messagingapp.app.createtopic;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messagingapp.app.R;
import com.example.messagingapp.app.topic.TopicActivity;
import com.example.messagingapp.app.topiclist.TopicListActivity;
import com.example.messagingapp.app.util.strategies.IErrorMessageStrategy;
import com.example.messagingapp.app.util.strategies.SeriousErrorMessageStrategy;
import com.example.messagingapp.eventDeliverySystem.User;
import com.example.messagingapp.eventDeliverySystem.filesystem.FileSystemException;
import com.example.messagingapp.eventDeliverySystem.server.ServerException;

public class CreateTopicActivity extends AppCompatActivity {
    private static final String TAG = "Create Topic";
    private final IErrorMessageStrategy errorMessageStrategy = new SeriousErrorMessageStrategy(this, R.string.ok);

    private User user;
    private EditText topicNameEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_topic);

        user = (User) getIntent().getExtras().getSerializable(TopicListActivity.ARG_USER);
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
                toNextActivity(topicName);
            }
        } catch (ServerException | FileSystemException e) {
            closeWithFatalError(e);
        }
    }

    private void closeWithFatalError(Exception e) {
        //ErrorDialogFragment.startFromException(getFragmentManager(), e); crashes on error lol
        errorMessageStrategy.showError("Error while creating topic.");
        Log.e(TAG, "create remote topic error", e);
    }

    private void toNextActivity(String topicName) {
        Intent intent = new Intent(this, TopicActivity.class);
        intent.putExtra(TopicActivity.ARG_USER, user);
        intent.putExtra(TopicActivity.ARG_TOPIC_NAME, topicName);
        startActivity(intent);
    }
}