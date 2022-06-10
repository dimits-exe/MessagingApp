package com.example.messagingapp.app.login;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.messagingapp.app.R;
import com.example.messagingapp.app.topiclist.TopicListActivity;
import com.example.messagingapp.app.util.strategies.IErrorMessageStrategy;
import com.example.messagingapp.app.util.strategies.SeriousErrorMessageStrategy;
import com.example.messagingapp.eventDeliverySystem.User;
import com.example.messagingapp.eventDeliverySystem.filesystem.FileSystemException;
import com.example.messagingapp.eventDeliverySystem.server.ServerException;
import com.google.android.material.textfield.TextInputLayout;

import java.net.UnknownHostException;
import java.util.Objects;

/**
 * An activity which creates or loads the user's data, connects with the server
 * and starts the main app activity.
 *
 * @author Dimitris Tsirmpas
 */
public class LoginActivity extends AppCompatActivity {
    public static final String ARG_IP = "IP";
    public static final String ARG_PORT = "PORT";

    private final IErrorMessageStrategy errorMessageStrategy =
            new SeriousErrorMessageStrategy(this.getBaseContext(), R.string.ok);

    private TextInputLayout usernameEditText;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.login_username_input);

        findViewById(R.id.login_create_button).setOnClickListener(e -> onSubmit(true));
        findViewById(R.id.login_login_button).setOnClickListener(e -> onSubmit(false));
    }

    /**
     * Collects user parameters, creates a user instance, and if successful, launches the main app.
     * @param newUser true if the user is created now, false otherwise
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onSubmit(boolean newUser) {
        String username = Objects.requireNonNull(usernameEditText.getEditText())
                .getText().toString();

        Intent old = getIntent();
        String serverIp = old.getStringExtra(LoginActivity.ARG_IP);
        int serverPort = old.getIntExtra(LoginActivity.ARG_PORT, -1);

        User user = tryCreateUser(newUser, serverIp, serverPort, username);

        if(user != null) {
            toNextActivity(user);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private User tryCreateUser(boolean newUser, String serverIp, int serverPort, String username) {
        User user = null;
        try {
            if (newUser)
                user = User.createNew(serverIp, serverPort, getFilesDir().toPath(), username);
            else
                user = User.loadExisting(serverIp, serverPort, getFilesDir().toPath(), username);
        } catch (ServerException e) {
            errorMessageStrategy.showError("Connection interrupted with the server.");
            Log.e("User Create", String.valueOf(e));
        } catch (FileSystemException e) {
            errorMessageStrategy.showError("Can't access the Android File System.");
            Log.e("User Create", String.valueOf(e));
        } catch (UnknownHostException e) {
            errorMessageStrategy.showError("Couldn't establish a connection with the server.");
            Log.e("User Create", String.valueOf(e));
        }

        return user;
    }

    private void toNextActivity(User user) {
        Intent intent = new Intent(this, TopicListActivity.class);
        intent.putExtra(TopicListActivity.ARG_USER, user);
        startActivity(intent);
    }
}