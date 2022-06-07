package com.example.messagingapp.app.topic;

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
    private final User user;
    private final String topicName;

    public TopicPresenter(IErrorMessageStrategy errorMessageStrategy, User user, String topicName) {
        this.errorMessageStrategy = errorMessageStrategy;
        this.user = user;
        this.topicName = topicName;
    }

    public void addFile() {
        File file = null;

        try {
            file = chooseFile();
        } catch (IOException ioe){
            errorMessageStrategy.showError("Error on retrieving file");
            Log.wtf("Topic Send File", ioe);
        }

        if(file != null)
            trySendFile(file);
    }

    public void sendText(String text) {
        //TODO: implement
    }

    public void takeAndSendPhoto() {
       File photo = takePhoto();
       trySendFile(photo);
    }

    private void trySendFile(File file) {
        try {
            sendFile(file);
        } catch (IOException ioe) {
            errorMessageStrategy.showError("Error on sending file");
            Log.wtf("Topic Send File", ioe);
        }
    }

    private File takePhoto() {
        //TODO: implement
        return null;
    }

    private void sendFile(File file) throws IOException {
        //TODO: implement
    }

    private File chooseFile() throws IOException {
        //TODO: implement
        return null;
    }
}
