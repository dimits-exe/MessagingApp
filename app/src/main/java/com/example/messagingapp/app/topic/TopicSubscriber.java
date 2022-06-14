package com.example.messagingapp.app.topic;

import com.example.messagingapp.app.util.strategies.IErrorMessageStrategy;
import com.example.messagingapp.eventDeliverySystem.ISubscriber;

/**
 * A subscriber that refreshes the page every time something is posted.
 *
 * @author Dimitris Tsirmpas
 */
class TopicSubscriber implements ISubscriber {
    private final String topicName;
    private final ITopicView view;
    private final IErrorMessageStrategy errorMessageStrategy;

    /**
     * Create a new subscriber for the current topic.
     * @param topicName the topic's name
     * @param view the view object that communicates with the TopicActivity
     * @param errorMessageStrategy the strategy with which error messages are shown to the user
     */
    public TopicSubscriber(String topicName, ITopicView view, IErrorMessageStrategy errorMessageStrategy){
        this.topicName = topicName;
        this.view = view;
        this.errorMessageStrategy = errorMessageStrategy;
    }

    @Override
    public void notify(String topicName) {
        if(topicName.equals(this.topicName))
            view.refresh();
    }

    @Override
    public void failure(String topicName) {
        if(topicName.equals(this.topicName))
            errorMessageStrategy.showError("An error has occurred");
    }
}
