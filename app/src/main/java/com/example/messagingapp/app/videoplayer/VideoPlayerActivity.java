package com.example.messagingapp.app.videoplayer;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messagingapp.app.R;
import com.example.messagingapp.app.util.strategies.IErrorMessageStrategy;
import com.example.messagingapp.app.util.strategies.SeriousErrorMessageStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * An activity that plays a selected video.
 *
 * @author Dimitris Tsirmpas
 */
public class VideoPlayerActivity extends AppCompatActivity {
    public static final String ARG_VIDEO = "VIDEO";
    private static final String TAG = "Video Player";

    private final IErrorMessageStrategy errorMessageStrategy =
            new SeriousErrorMessageStrategy(this, R.string.ok);
    private File tempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        // get video
        if(savedInstanceState == null || savedInstanceState.get(ARG_VIDEO) == null){
            throw new IllegalStateException("No video provided");
        }
        byte[] videoData = savedInstanceState.getByteArray(ARG_VIDEO);

        // to file
        tempFile = generateTempFile();
        writeToFile(tempFile, videoData);
        Uri video = getUriFromFile(tempFile);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deleteTempFile();
    }


    private File generateTempFile(){
        return new File(getFilesDir(), "tempVideo");
    }

    private void writeToFile(File tempFile, byte[] video) {
        try(FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(video);
        } catch (IOException e) {
            errorMessageStrategy.showError("Error while playing video");
            Log.e(TAG, "Write to file", e);
        }
    }

    private Uri getUriFromFile(File file){
        return Uri.fromFile(file);
    }

    private void deleteTempFile(){
        boolean success = tempFile.delete();

        if(!success) {
            Log.e(TAG, "Could not delete temp file");
        }
    }

    private String queryName(Uri uri) {
        Cursor returnCursor =
                getContentResolver().query(uri, null, null, null, null);
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        returnCursor.close();
        return name;
    }
}