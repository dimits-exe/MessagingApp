package com.example.messagingapp.app.topic;

/**
 * An interface through which controllers / presenters can request services from
 * {@link TopicActivity}.
 *
 * @author Dimitris Tsirmpas
 */
interface ITopicView {
    /**
     * Request that the TopicActivity begins playback of a selected video.
     * @param data the video's byte data
     */
    void playVideo(byte[] data);

    /**
     * Request that the TopicActivity refreshes the contents of it's displayed post feed.
     */
    void refresh();
}
