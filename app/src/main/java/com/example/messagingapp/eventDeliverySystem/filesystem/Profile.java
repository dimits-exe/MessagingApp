package com.example.messagingapp.eventDeliverySystem.filesystem;

import com.example.messagingapp.eventDeliverySystem.datastructures.Post;
import com.example.messagingapp.eventDeliverySystem.datastructures.Topic;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A data structure holding information about a user and their subscribed
 * topics.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
public class Profile implements Serializable {

	private final String               name;
	private final Map<String, Topic>   topics;
	private final Map<String, Integer> unreadTopics;

	/**
	 * Creates a new, empty, Profile with the specified name.
	 *
	 * @param name the unique name of the Profile
	 */
	public Profile(String name) {
		this(name, new HashMap<>());
	}

	private Profile(String name, Map<String, Topic> topics) {
		this.name = name;
		this.topics = topics;
		unreadTopics = new HashMap<>();

		// TODO: remove
		addTopic("topic1");
		addTopic("topic2");
		addTopic("topic3");
		addTopic("topic4");

		markUnread("topic1");
		markUnread("topic1");
		markUnread("topic2");
		markUnread("topic2");
		markUnread("topic2");
		markUnread("topic4");

		List<Post> posts = new LinkedList<>();
		posts.add(Post.fromText("first message", this.name));
		posts.add(Post.fromText("second message", this.name));
		posts.add(Post.fromText("third message", "lmao"));
		posts.add(Post.fromText("fourth message", this.name));
		posts.add(Post.fromText("fifth message", "lmao"));
		topics.get("topic1").post(posts);
	}

	/**
	 * Returns this Profile's name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns a Topic of this Profile.
	 * @param topicName the name of the Topic
	 *
	 * @return the Topic with that name
	 *
	 * @throws NoSuchElementException if no Topic with the given name exists
	 */
	public Topic getTopic(String topicName) {
		assertTopicExists(topicName);
		return topics.get(topicName);
	}

	/**
	 * Returns this Profile's topics.
	 *
	 * @return the topics
	 */
	public Map<String, Topic> getTopics() {
		return new HashMap<>(topics);
	}

	/**
	 * Adds a new, unique, Topic to this Profile.
	 *
	 * @param topicName the name of the new Topic
	 *
	 * @throws IllegalArgumentException if a Topic with the same name already exists
	 */
	public void addTopic(String topicName) {
		addTopic(new Topic(topicName));
	}

	/**
	 * Adds a new Topic to this Profile.
	 *
	 * @param topic the Topic
	 *
	 * @throws NullPointerException     if {@code topic == null}
	 * @throws IllegalArgumentException if a Topic with the same name already exists
	 */
	public void addTopic(Topic topic) {
		if (topic == null)
			throw new NullPointerException("Topic can't be null");

		final String topicName = topic.getName();
		if (topics.containsKey(topicName))
			throw new IllegalArgumentException("Topic with name " + topicName + " already exists");

		topics.put(topicName, topic);
		unreadTopics.put(topicName, 0);
	}

	/**
	 * Updates a Topic of this Profile with new Posts.
	 *
	 * @param topicName the name of the Topic to update
	 * @param posts     the new Posts to post to the Topic
	 *
	 * @throws NoSuchElementException if no Topic with the given name exists
	 */
	public void updateTopic(String topicName, List<Post> posts) {
		assertTopicExists(topicName);
		topics.get(topicName).post(posts);
	}

	/**
	 * Removes a Topic from this Profile.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @throws NoSuchElementException if no Topic with the given name exists
	 */
	public void removeTopic(String topicName) {
		assertTopicExists(topicName);
		topics.remove(topicName);
	}

	/**
	 * Marks a Topic as unread.
	 *
	 * @param topicName the name of the Topic
	 */
	public void markUnread(String topicName) {
		unreadTopics.put(topicName, unreadTopics.get(topicName) + 1);
	}

	/**
	 * Returns the number of unread posts in a Topic.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @return the unread count
	 */
	public int getUnread(String topicName) {
		return unreadTopics.get(topicName);
	}

	/**
	 * Marks all posts in a Topic as read.
	 *
	 * @param topicName the name of the Topic
	 */
	public void clearUnread(String topicName) {
		unreadTopics.put(topicName, 0);
	}

	@Override
	public String toString() {
		final List<Object> topicNames = Arrays.asList(topics.keySet().toArray());
		return String.format("Profile [name=%s, topics=%s]", name, topicNames);
	}

	private void assertTopicExists(String topicName) {
		if (!topics.containsKey(topicName))
			throw new NoSuchElementException("No Topic with name " + topicName + " found");
	}
}
