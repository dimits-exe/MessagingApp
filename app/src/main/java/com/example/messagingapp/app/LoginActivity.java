package com.example.messagingapp.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {

    public static final String USERNAME = "USER";
    public static final String NEW_USER = "NEW_USER";

    private EditText usernameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.login_username_field);

        findViewById(R.id.login_create_button).setOnClickListener(e -> onSubmit(true));
        findViewById(R.id.login_login_button).setOnClickListener(e -> onSubmit(false));
    }

    public void onSubmit(boolean newUser) {

        String username = usernameEditText.getText().toString();

        Intent old = getIntent();
        String serverIp = old.getStringExtra(ConnectActivity.SERVER_IP);
        int serverPort = old.getIntExtra(ConnectActivity.SERVER_PORT, -1);

        Intent intent = new Intent(this, null /* TODO HomepageActivity.class */);
        intent.putExtra(ConnectActivity.SERVER_IP, serverIp);
        intent.putExtra(ConnectActivity.SERVER_PORT, serverPort);
        intent.putExtra(USERNAME, username);
        intent.putExtra(NEW_USER, newUser);

        // TODO: remove
        for (String key : intent.getExtras().keySet())
            System.out.printf("%s-%s%n", key, intent.getStringExtra(key));

        startActivity(intent);
    }
}