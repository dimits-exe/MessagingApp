package com.example.messagingapp.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.EditText;

public class CreateTopicActivity extends AppCompatActivity {

    private User user;

    private EditText topicNameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_topic);

        user = getIntent().getExtras().get
        topicNameEditText = findViewById();
    }

    private void onSubmit() {

        String topicName = topicNameEditText.getText().toString();


    }
}