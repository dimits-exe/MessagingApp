package com.example.messagingapp.app.topiclist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.messagingapp.app.ConnectActivity;
import com.example.messagingapp.app.LoginActivity;
import com.example.messagingapp.app.R;
import com.example.messagingapp.eventDeliverySystem.User;
import com.example.messagingapp.eventDeliverySystem.filesystem.FileSystemException;
import com.example.messagingapp.eventDeliverySystem.server.ServerException;

import java.net.UnknownHostException;

public class TopicListActivity extends AppCompatActivity {

    private static final String EXCEPTION = "EXCEPTION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);

        Intent old = getIntent();
        String serverIp = old.getStringExtra(ConnectActivity.SERVER_IP);
        int serverPort = old.getIntExtra(ConnectActivity.SERVER_PORT, -1);
        String username = old.getStringExtra(LoginActivity.USERNAME);
        boolean newUser = old.getBooleanExtra(LoginActivity.NEW_USER, false);

        User user;
        try {
            if (newUser)
                user = User.createNew(serverIp, serverPort, null /* TODO */, username);
            else
                user = User.loadExisting(serverIp, serverPort, null /* TODO */, username);
        } catch (ServerException | FileSystemException | UnknownHostException e) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(EXCEPTION, e);

            DialogFragment edf = new ErrorDialogFragment();
            edf.setArguments(bundle);
            edf.show(getFragmentManager(), "errorDialog");

            return;
        }

        ((TextView) findViewById(R.id.topiclist_username)).setText(user.getCurrentProfile().getName());

        RecyclerView recyclerView = findViewById(R.id.topiclist_recycler_view);
        recyclerView.setAdapter(new TopicPreviewAdapter(user.getCurrentProfile()));
        recyclerView.setHasFixedSize(true);
    }

    public static class ErrorDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Exception e = (Exception) savedInstanceState.getSerializable(EXCEPTION);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Fatal Error Occurred")
                    .setMessage(e.getMessage())
                    .setCancelable(false)
                    .setIcon(R.drawable.error_icon)
                    .setNeutralButton("OK", (dialog, which) -> {});

            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            getActivity().finishAffinity();
        }
    }
}
