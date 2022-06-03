package com.example.messagingapp.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

/**
 * TODO
 */
public class ConnectActivity extends AppCompatActivity {

    public static final String SERVER_IP = "SERVER ADDRESS";
    public static final String SERVER_PORT = "SERVER PORT";

    private EditText ipEditText, portEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        ipEditText = findViewById(R.id.connect_server_ip_field);
        portEditText = findViewById(R.id.connect_server_port_field);

        findViewById(R.id.connect_button).setOnClickListener(e -> onSubmit());
    }

    /** TODO */
    public void onSubmit() {

        String serverIP = ipEditText.getText().toString();
        int serverPort = Integer.parseInt(portEditText.getText().toString());

        System.out.printf("Server IP: %s Server Port: %d%n", serverIP, serverPort);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(SERVER_IP, serverIP);
        intent.putExtra(SERVER_PORT, serverPort);

        startActivity(intent);
    }
}