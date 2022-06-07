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
    private TextInputLayout ipEditText, portEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        ipEditText = findViewById(R.id.connection_server_IP_input);
        portEditText = findViewById(R.id.connection_server_port_input);

        findViewById(R.id.connect_button).setOnClickListener(e -> onSubmit());
    }

    /**
     * Parse user provided arguments, check them, if correct launch login activity.
     */
    public void onSubmit() {
        //clear previous error messages
        ipEditText.setError(null);
        portEditText.setError(null);

        //extract user provided data
        String serverIP = Objects.requireNonNull(ipEditText.getEditText()).getText().toString();
        String serverPort = Objects.requireNonNull(portEditText.getEditText()).getText().toString();

        if(!checkParams(serverIP, serverPort)) {
            return;
        }

        int port = Integer.parseInt(serverPort);

        System.out.printf("Server IP: %s Server Port: %d%n", serverIP, port);
        toNextActivity(serverIP, port);
    }

    private boolean checkParams(String serverIP, String serverPort) {
        boolean success = true;
        if(!presenter.checkIP(serverIP)){
            success = false;
            ipEditText.setError("Wrong IP format");
        }

        if(!presenter.checkPort(serverPort)){
            success = false;
            portEditText.setError(serverPort + " is not a valid number");
        }

        return success;
    }

    private void toNextActivity(String serverIP, int serverPort){
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.ARG_IP, serverIP);
        intent.putExtra(LoginActivity.ARG_PORT, serverPort);

        startActivity(intent);
    }

}