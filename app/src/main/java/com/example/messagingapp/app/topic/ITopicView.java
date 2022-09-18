package com.example.messagingapp.app.topic;

import java.io.File;

/**
 * An interface through which controllers / presenters can request services from
 * {@link TopicActivity}.
 *
 * @author Dimitris Tsirmpas
 */
interface ITopicView {
    /**
     * Request that the TopicActivity begins playback of a selected video.
     * @param temp a temporary file holding the video's byte data
     */
    void playVideo(File temp);

    /**
     * Request that the TopicActivity refreshes the contents of it's displayed post feed.
     */
    void refresh();

    /**
     * Request that the app goes back to the main screen, because of an unrecoverable error.
     */
    void restartApp();
}
