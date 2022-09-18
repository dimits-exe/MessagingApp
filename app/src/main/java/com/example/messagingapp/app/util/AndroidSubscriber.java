package com.example.messagingapp.app.util;

import com.example.messagingapp.eventDeliverySystem.ISubscriber;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A composite implementation iof {@link ISubscriber} allowing dynamic addition and removal of
 * functionality depending on the currently shown activity.
 *
 * @author Dimitris Tsirmpas
 */
public final class AndroidSubscriber implements ISubscriber {
    private final List<ISubscriber> subscriberList = new LinkedList<>();

    /**
     * Add a new subscriber to the composite.
     * @param subscriber the new subscriber
     */
    public void add(ISubscriber subscriber) {
        subscriberList.add(subscriber);
    }

    /**
     * Remove a subscriber from the composite.
     * @param subscriber the subscriber to be removed
     * @throws NoSuchElementException if there was no such subscriber
     */
    public void remove(ISubscriber subscriber) throws NoSuchElementException {
        subscriberList.remove(subscriber);
    }

    @Override
    public void notify(String topicName) {
        for(ISubscriber subscriber : subscriberList)
            subscriber.notify(topicName);
    }

    @Override
    public void failure(String topicName) {
        for(ISubscriber subscriber: subscriberList)
            subscriber.failure(topicName);
    }
}
