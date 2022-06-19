package com.example.messagingapp.app.topic;

import android.util.Log;

import com.example.messagingapp.app.util.strategies.IErrorMessageStrategy;
import com.example.messagingapp.eventDeliverySystem.ISubscriber;

/**
 * A subscriber that refreshes the page every time something is posted.
 *
 * @author Dimitris Tsirmpas
 */
class TopicSubscriber implements ISubscriber {
    private static final String TAG = "TopicSubscriber";

    private final ITopicView view;
    private final IErrorMessageStrategy errorMessageStrategy;

    /**
     * Create a new subscriber for the current topic.
     * @param view the view object that communicates with the TopicActivity
     * @param errorMessageStrategy the strategy with which error messages are shown to the user
     */
    public TopicSubscriber(ITopicView view, IErrorMessageStrategy errorMessageStrategy){
        this.view = view;
        this.errorMessageStrategy = errorMessageStrategy;
    }

    @Override
    public void notify(String topicName) {
        Log.i(TAG, "Topic notified for update");
        view.refresh();
    }

    @Override
    public void failure(String topicName) {
        Log.i(TAG, "Topic notified for failure");
        errorMessageStrategy.showError("An error has occurred");
    }
}
