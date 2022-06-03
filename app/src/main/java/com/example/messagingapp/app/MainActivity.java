package com.example.messagingapp.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    public static final String SERVER_ADDRESS = "SERVER ADDRESS";
    public static final String SERVER_PORT = "SERVER PORT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * Get the connection info from the two labels and pass it onto the application.
     * This method is automatically called by the Submit button.
     * @param view the view
     */
    public void submitConnectionInfo(View view) {
        final TextView textViewIP = (TextView) view.findViewById(R.id.SERVER_IP_INPUT);
        final TextView textViewPort = (TextView) view.findViewById(R.id.SERVER_PORT_INPUT);

        String serverIP = (String) textViewIP.getText();
        int serverPort = Integer.parseInt((String) textViewPort.getText());
        //TODO: check if valid connection

        System.out.printf("Server IP: %s Server Port: %d", serverIP, serverPort);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(SERVER_ADDRESS, serverIP);
        intent.putExtra(SERVER_PORT, serverPort);

        startActivity(intent);
    }
}