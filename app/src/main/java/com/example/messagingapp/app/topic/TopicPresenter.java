package com.example.messagingapp.app.topic;

import android.net.Uri;
import android.util.Log;

import com.example.messagingapp.app.util.strategies.IErrorMessageStrategy;
import com.example.messagingapp.eventDeliverySystem.User;

import java.io.File;
import java.io.IOException;

/**
 * A class handling the logic behind the {@link TopicActivity}'s UI.
 *
 * @author Dimitris Tsirmpas
 */
class TopicPresenter {
    private final IErrorMessageStrategy errorMessageStrategy;
    private final File baseDir;
    private final User user;
    private final String topicName;

    public TopicPresenter(IErrorMessageStrategy errorMessageStrategy, File baseDir, User user, String topicName) {
        this.errorMessageStrategy = errorMessageStrategy;
        this.baseDir = baseDir;
        this.user = user;
        this.topicName = topicName;
    }

    public File getTempFileDir(){
        return new File(baseDir, "temp_file");
    }

    public void sendFile(Uri fileUri) {
        if(fileUri != null){
            trySendFile(new File(fileUri.getPath()));

            boolean success = getTempFileDir().delete();
            if(!success)
                Log.e("Topic", "Temp file not deleted");
        }
    }

    public void sendText(String text) {
        //TODO: implement
        Log.i("Placeholder", "Message " + text + " sent");
    }

    private void trySendFile(File file) {
        if(file == null)
            return;

        try {
            sendFile(file);
        } catch (IOException ioe) {
            errorMessageStrategy.showError("Error on sending file");
            Log.wtf("Topic Send File", ioe);
        }
    }

    private void sendFile(File file) throws IOException {
        //TODO: implement
        Log.i("Placeholder", "File " + file.toString() + " sent");
    }

}
