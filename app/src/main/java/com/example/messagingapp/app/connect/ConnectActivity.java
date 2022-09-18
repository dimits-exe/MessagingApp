package com.example.messagingapp.app.connect;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import com.example.messagingapp.app.R;
import com.example.messagingapp.app.login.LoginActivity;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

/**
 * An activity that gets the necessary connection information for the server.
 *
 * @author Dimitris Tsirmpas
 */
public class ConnectActivity extends AppCompatActivity {

    private final ConnectPresenter presenter = new ConnectPresenter();

    private TextInputLayout ipTextInputLayout, portTextInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        ipTextInputLayout = findViewById(R.id.connection_server_IP_input);
        portTextInputLayout = findViewById(R.id.connection_server_port_input);

        findViewById(R.id.connect_button).setOnClickListener(e -> onSubmit());
    }

    /** Validates the IP and Port and launches the Login Activity if they are correct */
    public void onSubmit() {
        // clear previous error messages
        ipTextInputLayout.setError(null);
        portTextInputLayout.setError(null);

        String serverIP = Objects.requireNonNull(ipTextInputLayout.getEditText()).getText().toString();
        String serverPort = Objects.requireNonNull(portTextInputLayout.getEditText()).getText().toString();

        if (!checkParams(serverIP, serverPort)) {
            return;
        }

        Integer port = Integer.valueOf(serverPort);

        System.out.printf("Server IP: %s Server Port: %d%n", serverIP, port);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.ARG_IP, serverIP);
        intent.putExtra(LoginActivity.ARG_PORT, port);

        startActivity(intent);
    }

    private boolean checkParams(String serverIP, String serverPort) {
        boolean success = true;
        if (!presenter.checkIP(serverIP)) {
            success = false;
            ipTextInputLayout.setError(serverIP + " is not a valid IPv4 address");
        }

        if (!presenter.checkPort(serverPort)) {
            success = false;
            portTextInputLayout.setError(serverPort + " is not a valid port number");
        }

        return success;
    }
}
