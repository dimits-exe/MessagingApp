package com.example.messagingapp.eventDeliverySystem;

import com.example.messagingapp.eventDeliverySystem.client.Publisher;
import com.example.messagingapp.eventDeliverySystem.datastructures.Post;
import com.example.messagingapp.eventDeliverySystem.filesystem.FileSystemException;
import com.example.messagingapp.eventDeliverySystem.filesystem.Profile;
import com.example.messagingapp.eventDeliverySystem.server.ServerException;

import java.util.NoSuchElementException;

public interface IUser {

    /**
     * Returns this User's current Profile.
     *
     * @return the current Profile
     */
    Profile getCurrentProfile();

    /**
     * Switches this User to manage a new Profile.
     *
     * @param profileName the name of the new Profile
     * @throws ServerException     if the connection to the server fails
     * @throws FileSystemException if an I/O error occurs while interacting with the
     *                             file system
     */
    void switchToNewProfile(String profileName) throws ServerException, FileSystemException;

    /**
     * Switches this User to manage an existing.
     *
     * @param profileName the name of an existing Profile
     * @throws ServerException     if the connection to the server fails
     * @throws FileSystemException if an I/O error occurs while interacting with the
     *                             file system
     */
    void switchToExistingProfile(String profileName)
            throws ServerException, FileSystemException;

    /**
     * Posts a Post to a Topic.
     *
     * @param post      the Post to post
     * @param topicName the name of the Topic to which to post
     * @see Publisher#push(Post, String)
     */
    void post(Post post, String topicName);

    /**
     * Attempts to push a new Topic. If this succeeds,
     * {@link #listenForNewTopic(String)} is called.
     *
     * @param topicName the name of the Topic to create
     * @return {@code true} if it was successfully created, {@code false} otherwise
     * @throws ServerException          if the connection to the server fails
     * @throws FileSystemException      if an I/O error occurs while interacting with the
     *                                  file system
     * @throws IllegalArgumentException if a Topic with the same name already exists
     */
    boolean createTopic(String topicName) throws ServerException, FileSystemException;

    /**
     * Pulls all new Posts from a Topic, adds them to the Profile and saves them to
     * the file system. Posts that have already been pulled are not pulled again.
     *
     * @param topicName the name of the Topic from which to pull
     * @throws FileSystemException    if an I/O error occurs while interacting with
     *                                the file system
     * @throws NoSuchElementException if no Topic with the given name exists
     */
    void pull(String topicName) throws FileSystemException;

    /**
     * Registers a new Topic for which new Posts will be pulled and adds it to the
     * Profile and file system. The pulled topics will be added to the Profile and
     * saved to the file system.
     *
     * @param topicName the name of the Topic to listen for
     * @throws ServerException          if the connection to the server fails
     * @throws FileSystemException      if an I/O error occurs while interacting
     *                                  with the file system
     * @throws NullPointerException     if topic == null
     * @throws IllegalArgumentException if a Topic with the same name already exists
     */
    void listenForNewTopic(String topicName) throws ServerException, FileSystemException;

    /**
     * Registers an existing Topic from the file system for which new Posts will be pulled.
     * The pulled topics will be added to the Profile and
     * saved to the file system.
     *
     * @param topicName the name of the Topic to listen for
     * @throws ServerException          if the connection to the server fails
     * @throws NullPointerException     if there is no such topic
     * @throws IllegalArgumentException if a Topic with the same name already exists
     */
    void listenForExistingTopic(String topicName) throws ServerException;

    /**
     * Return the assigned subscriber for this user instance.
     *
     * @return the assigned subscriber instance
     */
    ISubscriber getSubscriber();
}
