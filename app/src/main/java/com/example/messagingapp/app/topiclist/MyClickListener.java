package com.example.messagingapp.app.topiclist;

/**
 * Allows the TopicListActivity to provide a callback for the TopicListAdapter when the user clicks
 * on an item of the Adapter, when the user wants to view the contents of a conversation.
 *
 * @author Alex Mandelias
 */
interface MyClickListener {

    /**
     * Should be called when the user clicks on a conversation. This should start a TopicActivity
     * which displays that conversation.
     *
     * @param topicName the name of the Topic to display
     */
    void onClick(String topicName);

}
