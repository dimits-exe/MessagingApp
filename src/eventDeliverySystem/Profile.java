package eventDeliverySystem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A data structure holding information about a user and their subscribed
 * topics.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
class Profile {

	private final String             name;
	private final long               id;
	private final Map<String, Topic> topics;
	private final Map<String, Integer> unreadTopics;

	/**
	 * Creates a new, empty, Profile with the specified name and a random unique ID.
	 * This constructor should be used when creating a new Profile, since the ID is
	 * not known and is randomly generated.
	 *
	 * @param name the name of the Profile
	 */
	public Profile(String name) {
		this(name, ThreadLocalRandom.current().nextLong(), new HashMap<>());
	}

	/**
	 * Creates a new, empty, Profile with the specified name and unique ID. This
	 * constructor should be used when loading a saved Profile from disk, since the
	 * ID is known.
	 *
	 * @param name the name of the Profile
	 * @param id   the Profile's unique ID
	 */
	public Profile(String name, long id) {
		this(name, id, new HashMap<>());
	}

	private Profile(String name, long id, Map<String, Topic> topics) {
		this.name = name;
		this.id = id;
		this.topics = topics;
		unreadTopics = new HashMap<>();
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
	 * Returns this Profile's id.
	 *
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * Returns this Profile's topics.
	 *
	 * @return the topics
	 */
	public Map<String, Topic> getTopics() {
		// TODO: should this return a copy (a deep copy)?
		// In this case we need to have a Topic(Topic) copy-constructor
		return topics;
	}

	// TODO: maybe not throw IllegalArgumentException and return boolean?

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
		final Topic topic = topics.get(topicName);
		if (topic == null)
			throw new NoSuchElementException("No Topic with name " + topicName + " found");

		topic.post(posts);
	}

	/**
	 * Removes a Topic from this Profile.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @throws NoSuchElementException if no Topic with the given name exists
	 */
	public void removeTopic(String topicName) {
		if (topics.remove(topicName) == null)
			throw new NoSuchElementException("No Topic with name " + topicName + " found");
	}

	public void markUnread(String topicName) {
		unreadTopics.put(topicName, unreadTopics.get(topicName) + 1);
	}

	public void clearUnread(String topicName) {
		unreadTopics.put(topicName, 0);
	}

	@Override
	public String toString() {
		final List<Object> topicNames = Arrays.asList(topics.keySet().toArray());
		return String.format("Profile [name=%s, id=%d, topics=%s]", name, id, topicNames);
	}
}
