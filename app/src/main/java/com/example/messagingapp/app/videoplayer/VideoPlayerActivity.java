package com.example.messagingapp.app.videoplayer;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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
        File videoData = (File) getIntent().getSerializableExtra(ARG_VIDEO);

        // to file
        Uri video = getUriFromFile(videoData);

        // set up label
        TextView videoLabel = findViewById(R.id.videoplayer_video_name_label);
        videoLabel.setText(queryName(video));

        // set up video view
        VideoView videoView;
        videoView = findViewById(R.id.video_player_video_view);
        videoView.setVideoURI(video);
        videoView.setMediaController(new MediaController(this));
        videoView.setOnCompletionListener(mediaPlayer -> {
            // do nothing
        });
    }

    private Uri getUriFromFile(File file){
        return Uri.fromFile(file);
    }


    private String queryName(Uri uri) {
        Cursor returnCursor =
                getContentResolver().query(uri, null, null, null, null);

        if (returnCursor == null){
            return "video";
        }

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        returnCursor.close();
        return name;
    }
}