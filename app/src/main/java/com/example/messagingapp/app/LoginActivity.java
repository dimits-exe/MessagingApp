package com.example.messagingapp.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {

    public static final String USERNAME = "USERNAME";

    private EditText usernameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.connect_server_ip_field);
    }

    public void onSubmit() {

        String username = usernameEditText.getText().toString();

        System.out.printf("Username: %s%n", username);

        Intent old = getIntent();

        Intent intent = new Intent(this, null /* TODO HomepageActivity.class */);
        intent.putExtra(USERNAME, username);
        intent.putExtra(ConnectActivity.SERVER_IP, old.getStringExtra(ConnectActivity.SERVER_IP));
        intent.putExtra(ConnectActivity.SERVER_PORT, old.getStringExtra(ConnectActivity.SERVER_PORT));

        startActivity(intent);
    }
}