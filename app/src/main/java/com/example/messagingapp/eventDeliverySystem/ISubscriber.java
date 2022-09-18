package com.example.messagingapp.eventDeliverySystem;

import java.io.Serializable;

/**
 * Any object that can be used to notify this User about an event for a Topic.
 *
 * @author Alex Mandelias
 */
public interface ISubscriber extends Serializable {

    /**
     * Notifies this User about an event for a Topic.
     *
     * @param topicName the name of the Topic
     */
    void notify(String topicName);

    /**
     * Notifies this User about failure to send a Post to a Topic.
     *
     * @param topicName the name of the Topic
     */
    void failure(String topicName);
}
