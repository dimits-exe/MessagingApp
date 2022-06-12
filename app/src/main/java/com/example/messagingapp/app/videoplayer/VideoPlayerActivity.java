package com.example.messagingapp.app.videoplayer;

import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messagingapp.app.R;

import java.io.File;

/**
 * An activity that plays a selected video.
 *
 * @author Dimitris Tsirmpas
 */
public class VideoPlayerActivity extends AppCompatActivity {
    public static final String ARG_VIDEO = "VIDEO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        // get video
        if(savedInstanceState == null || savedInstanceState.get(ARG_VIDEO) == null){
            throw new IllegalStateException("No video provided");
        }

        Uri video = (Uri) savedInstanceState.getSerializable(ARG_VIDEO);

        // set up label
        TextView videoLabel = findViewById(R.id.videoplayer_video_name_label);
        videoLabel.setText(queryName(video));

        // set up video view
        VideoView videoView;
        videoView = findViewById(R.id.video_player_video_view);
        videoView.setVideoURI(video);
        videoView.setMediaController(new MediaController(this));
        videoView.setOnCompletionListener(mediaPlayer -> {
            // on completion do nothing
        });
    }

    private String queryName(Uri uri) {
        return new File(uri.getPath()).getName();
    }
}