package com.example.messagingapp.eventDeliverySystem.util;

import com.example.messagingapp.eventDeliverySystem.datastructures.Packet;
import com.example.messagingapp.eventDeliverySystem.datastructures.Post;
import com.example.messagingapp.eventDeliverySystem.datastructures.PostInfo;

import java.io.Serializable;

/**
 * An interface denoting any class that needs to be notified about the arrival
 * of a part of or a whole {@link Post}.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
public interface Subscriber extends Serializable {

	/**
	 * Notifies the object that a post has arrived, concerning a certain Topic.
	 *
	 * @param postInfo  the information of the arrived post
	 * @param topicName the name of the Topic
	 */
	void notify(PostInfo postInfo, String topicName);

	/**
	 * Notifies the object that a part of a post has arrived, concerning a certain
	 * Topic.
	 *
	 * @param packet    the part of the post
	 * @param topicName the name of the Topic
	 */
	void notify(Packet packet, String topicName);
}
