package com.example.messagingapp.app.topic;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messagingapp.app.R;
import com.example.messagingapp.app.connect.ConnectActivity;
import com.example.messagingapp.app.util.AndroidSubscriber;
import com.example.messagingapp.app.util.LoggedInUserHolder;
import com.example.messagingapp.app.util.strategies.IErrorMessageStrategy;
import com.example.messagingapp.app.util.strategies.MinorErrorMessageStrategy;
import com.example.messagingapp.app.videoplayer.VideoPlayerActivity;
import com.example.messagingapp.eventDeliverySystem.IUser;
import com.example.messagingapp.eventDeliverySystem.datastructures.Topic;
import com.example.messagingapp.eventDeliverySystem.filesystem.FileSystemException;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

/**
 * An activity which displays messages from a given {@link Topic}
 * and allows the user to send its own.
 *
 * @author Dimitris Tsirmpas
 */
public class TopicActivity extends AppCompatActivity {
    public static final String ARG_TOPIC_NAME = "TOPIC";

    private static final String TAG = "Topic";

    private TopicPresenter presenter;
    private TopicAdapter adapter;
    private TopicSubscriber subscriber;
    private EditText messageTextArea;

    private String topicName;
    private IUser user;

    private Uri photoFileUri;
    private Uri videoFileUri;
    private ActivityResultLauncher<Uri> photoLauncher;
    private ActivityResultLauncher<Uri> videoLauncher;
    private ActivityResultLauncher<String> fileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic);

        user = LoggedInUserHolder.getInstance();
        topicName = getIntent().getStringExtra(ARG_TOPIC_NAME);
        IErrorMessageStrategy errorMessageStrategy = new MinorErrorMessageStrategy(this);
        TopicView view = new TopicView();

        setUpPresenter(user, topicName, view, errorMessageStrategy);
        setUpFields(topicName);
        setUpLaunchers();
        setUpListeners();
        setUpPostList(view);
        setUpNotificationsManager(user, view, errorMessageStrategy);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // remove the subscriber for this topic
        ((AndroidSubscriber) user.getSubscriber()).remove(subscriber);
    }

    private void setUpPresenter(IUser user, String topicName, ITopicView view ,IErrorMessageStrategy errorMessageStrategy) {
        presenter = new TopicPresenter(errorMessageStrategy, view, getCacheDir(), user, topicName);
    }

    private void setUpFields(String topicName) {
        TextView label = findViewById(R.id.topic_label);
        label.setText(topicName);
        messageTextArea = findViewById(R.id.topic_message_input);
    }

    private void setUpLaunchers() {
        photoFileUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider", presenter.getNewTempFile(".jpg"));

        videoFileUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider", presenter.getNewTempFile(".mp4"));

        photoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), result -> {
                    if(result)
                        presenter.sendFile(photoFileUri, getContentResolver());
                });

        videoLauncher = registerForActivityResult(
                new ActivityResultContracts.CaptureVideo(), result -> {
                    if(result)
                        presenter.sendFile(videoFileUri, getContentResolver());
                });

        fileLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> presenter.sendFile(uri, getContentResolver()));
    }

    private void setUpListeners() {
        setUpCameraListener();
        setUpFileListener();
        setUpMessageListener();
    }

    @SuppressLint("NonConstantResourceId")
    private void setUpFileListener() {
        FloatingActionButton addFilesButton = findViewById(R.id.topic_add_file_button);
        addFilesButton.setOnClickListener(v-> {
            PopupMenu popup = new PopupMenu(this, v);

            popup.setOnMenuItemClickListener(menuItem -> {
                switch(menuItem.getItemId()) {
                    case R.id.topic_menu_file_photo_item:
                        selectPhoto();
                        return true;
                    case R.id.topic_menu_file_video_item:
                        selectVideo();
                        return true;
                    default:
                        return false;
                }
            });
            popup.inflate(R.menu.file_menu);
            popup.show();
        });
    }

    private void selectPhoto() {
        fileLauncher.launch("image/*");
    }

    private void selectVideo(){
        fileLauncher.launch("video/*");
    }

    @SuppressLint("NonConstantResourceId")
    private void setUpCameraListener() {
        FloatingActionButton takePhotoButton = findViewById(R.id.topic_take_photo_button);
        takePhotoButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);

            popup.setOnMenuItemClickListener(menuItem -> {
                switch(menuItem.getItemId()) {
                    case R.id.topic_menu_item_photo:
                        takePhoto();
                        return true;
                    case R.id.topic_menu_item_video:
                        takeVideo();
                        return true;
                    default:
                        return false;
                }
            });
            popup.inflate(R.menu.camera_menu);
            popup.show();
        });
    }

    private void takePhoto() {
        photoLauncher.launch(photoFileUri);
    }

    private void takeVideo() {
        videoLauncher.launch(videoFileUri);
    }

    private void setUpMessageListener() {
        FloatingActionButton sendMessageButton = findViewById(R.id.topic_send_message_button);
        sendMessageButton.setOnClickListener(view -> sendTextMessage());
    }

    private void setUpPostList(ITopicView view){
        adapter = new TopicAdapter(presenter, view, new MinorErrorMessageStrategy(this));
        ((RecyclerView) findViewById(R.id.topic_recycler_view)).setAdapter(adapter);
    }

    private void setUpNotificationsManager(IUser user, ITopicView view,
                                           IErrorMessageStrategy errorMessageStrategy) {
        subscriber = new TopicSubscriber(view, errorMessageStrategy);
        ((AndroidSubscriber) user.getSubscriber()).add(subscriber);
    }

    private void sendTextMessage() {
        presenter.sendText(messageTextArea.getText().toString());
        messageTextArea.setText("");
    }


    /**
     * A default implementation of the {@link ITopicView} interface.
     *
     * @author Dimitris Tsirmpas
     */
    private final class TopicView implements ITopicView {

        @Override
        public void playVideo(File temp) {
            Intent intent = new Intent(TopicActivity.this, VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.ARG_VIDEO, temp);
            startActivity(intent);
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public synchronized void refresh() {
            try {
                user.pull(TopicActivity.this.topicName);
            } catch (FileSystemException e) {
                Log.e(TAG, "Refresh", e);
            }
            runOnUiThread(()->{
                //TODO: update only new posts
                TopicActivity.this.adapter.updatePosts(user.getCurrentProfile().getTopic(TopicActivity.this.topicName).getAllPosts());
                TopicActivity.this.adapter.notifyDataSetChanged();
                Log.i(TAG, "Page refreshed");
            });

        }

        @Override
        public void restartApp() {
            Intent intent = new Intent(TopicActivity.this, ConnectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }

}