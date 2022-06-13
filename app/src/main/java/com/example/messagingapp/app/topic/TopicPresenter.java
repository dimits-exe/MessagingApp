package com.example.messagingapp.app.topic;

import android.net.Uri;
import android.util.Log;

import com.example.messagingapp.app.util.strategies.IErrorMessageStrategy;
import com.example.messagingapp.eventDeliverySystem.User;
import com.example.messagingapp.eventDeliverySystem.datastructures.Post;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * A class handling the logic behind the {@link TopicActivity}'s UI.
 *
 * @author Dimitris Tsirmpas
 */
class TopicPresenter {
    private static final String TAG = "TopicPresenter";

    private final IErrorMessageStrategy errorMessageStrategy;
    private final File baseDir;
    private final User user;
    private final String topicName;

    private File tempFile = null;

    /**
     * Create a new TopicPresenter.
     * @param errorMessageStrategy the strategy with which error messages are shown to the user
     * @param baseDir the base file directory of the application
     * @param user the logged-in user instance
     * @param topicName the name of the current topic
     */
    public TopicPresenter(IErrorMessageStrategy errorMessageStrategy, File baseDir, User user, String topicName) {
        this.errorMessageStrategy = errorMessageStrategy;
        this.baseDir = baseDir;
        this.user = user;
        this.topicName = topicName;
    }

    /**
     * Returns the name of the current Profile.
     *
     * @return the name
     */
    public String getProfileName() {
        return user.getCurrentProfile().getName();
    }

    /**
     * Returns all the Posts of the current Profile for the Topic this presenter is responsible for.
     *
     * @return a list with the Posts
     */
    public List<Post> getProfilePosts() {
        return user.getCurrentProfile().getTopic(topicName).getAllPosts();
    }

    /**
     * Get a temporary file. The file will be deleted automatically on the next method call or
     * on VM exit.
     *
     * @return a new temporary file
     */
    public synchronized File getTempFileDir(){
        if(tempFile != null){
            try {
                Files.deleteIfExists(tempFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            tempFile = File.createTempFile("temp","temp_file", baseDir);
            tempFile.deleteOnExit();
        } catch (IOException e) {
            Log.wtf(TAG, e);
            System.exit(-1);
        }
        return tempFile;
    }

    /**
     * Send a file to the topic. Displays an error to the user if any error occurs.
     * @param fileUri the uri of the file
     */
    public void sendFile(Uri fileUri) {
        if(fileUri != null){
            try {
                Post post = Post.fromFile(new File(fileUri.getPath()), user.getCurrentProfile().getName());
                trySendPost(post);
            } catch (IOException e) {
                errorMessageStrategy.showError("An error occured while sending the file");
                Log.e(TAG, "Send file", e);
            }
            
        }
    }

    /**
     * Send a text message to the topic. Displays an error to the user if any error occurs.
     * @param text the text message
     */
    public void sendText(String text) {
        Post post = Post.fromText(text, user.getCurrentProfile().getName());
        trySendPost(post);
        Log.i(TAG, "Message " + text + " sent");
    }

    /**
     * Send a {@link Post}, displaying an error to the user if any error occurs.
     * @param post the post to be sent
     */
    private void trySendPost(Post post) {
        try {
            sendFile(post);
        } catch (IOException ioe) {
            errorMessageStrategy.showError("Error on sending file");
            Log.e(TAG, "send file", ioe);
        }
    }

    /**
     * Send a {@link Post} to the remote topic.
     * @param post the post to be sent
     * @throws IOException if any error occurs while transmitting the post
     */
    private void sendFile(Post post) throws IOException {
        user.post(post, topicName);
        Log.i(TAG,"File " + post.toString() + " sent");
    }

}
