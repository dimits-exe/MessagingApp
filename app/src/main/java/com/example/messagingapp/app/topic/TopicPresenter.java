package com.example.messagingapp.app.topic;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import com.example.messagingapp.app.util.AndroidSubscriber;
import com.example.messagingapp.app.util.strategies.IErrorMessageStrategy;
import com.example.messagingapp.eventDeliverySystem.IUser;
import com.example.messagingapp.eventDeliverySystem.User;
import com.example.messagingapp.eventDeliverySystem.datastructures.Post;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A class handling the logic behind the {@link TopicActivity}'s UI.
 *
 * @author Dimitris Tsirmpas
 */
class TopicPresenter {
    private static final String TAG = "TopicPresenter";

    private final IErrorMessageStrategy errorMessageStrategy;
    private final ITopicView view;
    private final File baseDir;
    private final IUser user;
    private final String topicName;


    /**
     * Create a new TopicPresenter.
     * @param errorMessageStrategy the strategy with which error messages are shown to the user
     * @param view the view object that communicates with the TopicActivity
     * @param baseDir the base file directory of the application
     * @param user the logged-in user instance
     * @param topicName the name of the current topic
     */
    public TopicPresenter(IErrorMessageStrategy errorMessageStrategy, ITopicView view,
                          File baseDir, IUser user, String topicName) {
        this.errorMessageStrategy = errorMessageStrategy;
        this.view = view;
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
     * Get a temporary file. The file will be deleted automatically on VM exit.
     *
     * @return a new temporary file
     */
    public File getNewTempFile() {
        File file = null;
        try {
            file = File.createTempFile("temp_file", UUID.randomUUID().toString(), baseDir);
            file.deleteOnExit();
        } catch (IOException e) {
            Log.wtf(TAG, e);
            System.exit(-1);
        }
        return file;
    }

    /**
     * Send a file to the topic. Displays an error to the user if any error occurs.
     * @param fileUri the uri of the file
     * @param resolver the content resolver for that file
     */
    public void sendFile(Uri fileUri, ContentResolver resolver) {
        /*
         * We can't access the file directly from the Uri, so we copy its contents to a
         * temporary file which we will send instead.
         *
         * Also this is an EXTREMELY slow procedure so we execute it in a thread.
         */

        Runnable fileSendProc = () -> {
            if(fileUri != null) {
                try {
                    File postContents = copyContentsToTemp(fileUri, resolver);
                    Post post = Post.fromFile(postContents, user.getCurrentProfile().getName());
                    trySendPost(post);

                    // refresh to show the user's content
                    view.refresh();
                } catch (IOException e) {
                    errorMessageStrategy.showError("An error occurred while sending the file");
                    Log.e(TAG, "Send file", e);
                }
            }

        };

        Executor exec = Executors.newSingleThreadExecutor();
        exec.execute(fileSendProc);
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
     * Copy the contents of the uri file to a temp file and return it.
     * @param uri the uri of the file to be sent
     * @param resolver the content provider of the application
     * @return a new temp file containing the contents of the uri
     * @throws IOException if the copy procedure fails
     */
    private File copyContentsToTemp(Uri uri, ContentResolver resolver) throws IOException {
        File temp = getNewTempFile();
        Files.copy(resolver.openInputStream(uri), temp.toPath(), REPLACE_EXISTING);
        return temp;
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
