package com.example.messagingapp.app.topic;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messagingapp.app.R;
import com.example.messagingapp.app.util.strategies.MinorErrorMessageStrategy;
import com.example.messagingapp.eventDeliverySystem.User;
import com.example.messagingapp.eventDeliverySystem.datastructures.Topic;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

/**
 * An activity which displays messages from a given {@link Topic}.
 *
 * @author Dimitris Tsirmpas
 */
public class TopicActivity extends AppCompatActivity {

    public static final String ARG_USER = "USER";
    public static final String ARG_TOPIC_NAME = "TOPIC";

    private TopicPresenter presenter;

    private EditText messageTextArea;

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic);

        String topicName = getIntent().getStringExtra(ARG_TOPIC_NAME);

        setUpPresenter(topicName);
        setUpFields(topicName);
        setUpListeners();

        recyclerView = findViewById(R.id.topic_recycler_view);
        recyclerView.setAdapter(new TopicAdapter(presenter));
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
        ActivityResultLauncher<String> fileLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> presenter.trySendFile(new File(uri.getPath())));

        FloatingActionButton addFilesButton = findViewById(R.id.topic_add_file_button);
        addFilesButton.setOnClickListener(view -> fileLauncher.launch("file/*"));

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