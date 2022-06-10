package com.example.messagingapp.app.topic;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.messagingapp.app.R;
import com.example.messagingapp.app.util.strategies.MinorErrorMessageStrategy;
import com.example.messagingapp.eventDeliverySystem.User;
import com.example.messagingapp.eventDeliverySystem.datastructures.Topic;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * An activity which displays messages from a given {@link Topic}.
 *
 * @author Dimitris Tsirmpas
 */
public class TopicActivity extends AppCompatActivity {

    public static final String ARG_USER = "USER";
    public static final String ARG_TOPIC = "TOPIC";

    private TopicPresenter presenter;

    private EditText messageTextArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic);

        String topicName = getIntent().getStringExtra(ARG_TOPIC);

        setUpPresenter(topicName);
        setUpFields(topicName);
        setUpListeners();
    }

    private void setUpPresenter(String topicName) {
        User user = (User) getIntent().getSerializableExtra(ARG_USER);
        presenter = new TopicPresenter(new MinorErrorMessageStrategy(getBaseContext()), user, topicName);
    }

    private void setUpFields(String topicName) {
        TextView label = findViewById(R.id.topic_label);
        label.setText(topicName);
        messageTextArea = findViewById(R.id.topic_message_input);
    }

    private void setUpListeners() {
        FloatingActionButton addFilesButton = findViewById(R.id.topic_add_file_button);
        addFilesButton.setOnClickListener(view -> presenter.addFile());

        FloatingActionButton sendMessageButton = findViewById(R.id.topic_send_message_button);
        sendMessageButton.setOnClickListener(view -> sendTextMessage());

        FloatingActionButton takePhotoButton = findViewById(R.id.topic_take_photo_button);
        takePhotoButton.setOnClickListener(view -> presenter.takeAndSendPhoto());
    }

    private void sendTextMessage() {
        presenter.sendText(messageTextArea.getText().toString());
        messageTextArea.setText("");
    }

}