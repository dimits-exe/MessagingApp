package com.example.messagingapp.app.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messagingapp.app.R;
import com.example.messagingapp.app.topiclist.TopicListActivity;
import com.example.messagingapp.app.util.AndroidSubscriber;
import com.example.messagingapp.app.util.LoggedInUserHolder;
import com.example.messagingapp.app.util.strategies.IErrorMessageStrategy;
import com.example.messagingapp.app.util.strategies.SeriousErrorMessageStrategy;
import com.example.messagingapp.eventDeliverySystem.User;
import com.example.messagingapp.eventDeliverySystem.filesystem.FileSystemException;
import com.example.messagingapp.eventDeliverySystem.server.ServerException;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
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

    private static final String TAG = "Login";

    private final IErrorMessageStrategy errorMessageStrategy =
            new SeriousErrorMessageStrategy(this, R.string.ok);

    private TextInputLayout usernameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.login_username_input);

        findViewById(R.id.login_create_button).setOnClickListener(e -> onSubmit(true));
        findViewById(R.id.login_login_button).setOnClickListener(e -> onSubmit(false));
    }

    /**
     * Attempts to create a new User instance and launches the main app on successful creation.
     *
     * @param newUser {@code true} to create a new user, {@code false} to log in as an existing user
     */
    public void onSubmit(boolean newUser) {
        String username = Objects.requireNonNull(usernameEditText.getEditText())
                .getText().toString();

        Intent old = getIntent();
        String serverIp = old.getStringExtra(LoginActivity.ARG_IP);
        int serverPort = old.getIntExtra(LoginActivity.ARG_PORT, -1);

        User user = tryCreateUser(newUser, serverIp, serverPort, username);

        if (user != null) {
            LoggedInUserHolder.getInstance().setUser(user);
            Intent intent = new Intent(this, TopicListActivity.class);
            startActivity(intent);
        }
    }

    private User tryCreateUser(boolean newUser, String serverIp, int serverPort, String username) {
        try {
            Path userDir = getFilesDir().toPath().resolve("users");
            if (!Files.exists(userDir)) {
                try {
                    Files.createDirectory(userDir);
                } catch (IOException e) {
                    throw new FileSystemException(userDir, e);
                }
            }

            if (newUser)
                return User.createNew(new AndroidSubscriber(), serverIp, serverPort, userDir, username);
            else
                return User.loadExisting(new AndroidSubscriber(), serverIp, serverPort, userDir, username);

        } catch (ServerException e) {
            errorMessageStrategy.showError("Connection interrupted with the server.");
            Log.e(TAG, String.valueOf(e), e);
        } catch (FileSystemException e) {
            errorMessageStrategy.showError("Can't access the Android File System.");
            Log.e(TAG, String.valueOf(e), e);
        } catch (UnknownHostException e) {
            errorMessageStrategy.showError("Couldn't establish a connection with the server.");
            Log.e(TAG, String.valueOf(e), e);
        } catch (NoSuchElementException e) {
            errorMessageStrategy.showError(e.getMessage());
        }

        return null;
    }
}
