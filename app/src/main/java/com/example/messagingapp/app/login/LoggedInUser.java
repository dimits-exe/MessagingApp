package com.example.messagingapp.app.login;

import com.example.messagingapp.eventDeliverySystem.ISubscriber;
import com.example.messagingapp.eventDeliverySystem.IUser;
import com.example.messagingapp.eventDeliverySystem.User;
import com.example.messagingapp.eventDeliverySystem.datastructures.Post;
import com.example.messagingapp.eventDeliverySystem.filesystem.FileSystemException;
import com.example.messagingapp.eventDeliverySystem.filesystem.Profile;
import com.example.messagingapp.eventDeliverySystem.server.ServerException;

/**
 * A wrapper class ensuring that the current {@link User} can only be changed
 * by the {@link LoginActivity} but accessed from every part of the application.
 * All access to the underlying user instance should be done through <b>this class only</b>.
 *
 * @author Dimitris Tsirmpas
 */
public class LoggedInUser implements IUser {
    private IUser user;

    public LoggedInUser() {
        user = null;
    }

    /**
     * Change which user is being used throughout the application.
     * @param user the user
     */
    void setUser(IUser user){
        this.user = user;
    }

    @Override
    public Profile getCurrentProfile() {
        throwOnNull();
        return user.getCurrentProfile();
    }

    @Override
    public void switchToNewProfile(String profileName) throws ServerException, FileSystemException {
        throwOnNull();
        user.switchToNewProfile(profileName);
    }

    @Override
    public void switchToExistingProfile(String profileName) throws ServerException, FileSystemException {
        throwOnNull();
        user.switchToExistingProfile(profileName);
    }

    @Override
    public void post(Post post, String topicName) {
        throwOnNull();
        user.post(post, topicName);
    }

    @Override
    public boolean createTopic(String topicName) throws ServerException, FileSystemException {
        throwOnNull();
        return user.createTopic(topicName);
    }

    @Override
    public void pull(String topicName) throws FileSystemException {
        throwOnNull();
        user.pull(topicName);
    }

    @Override
    public void listenForNewTopic(String topicName) throws ServerException, FileSystemException {
        throwOnNull();
        user.listenForNewTopic(topicName);
    }

    @Override
    public void listenForExistingTopic(String topicName) throws ServerException {
        throwOnNull();
        user.listenForExistingTopic(topicName);
    }

    @Override
    public ISubscriber getSubscriber() {
        throwOnNull();
        return user.getSubscriber();
    }

    private void throwOnNull(){
        if(user == null){
            throw new IllegalStateException("No user has been set");
        }
    }
}
