package eventDeliverySystem;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A data structure holding information about a user and
 * his subscribed topics.
 *
 */
class Profile {
	private final String name;
	private final long id;
	private final Set<Topic> subscribedTopics;
	
	/**
	 * Create a new user profile with the specified name.
	 * @param name the name of the user
	 */
	public Profile(String name) {
		this.name = name;
		this.id = ThreadLocalRandom.current().nextLong();
		this.subscribedTopics = new HashSet<Topic>();
	}
	
	/**
	 * Create a new user profile with the specified name and subscribed topics.
	 * @param name the username
	 * @param subscribedTopics a set of already subscribed topics
	 */
	public Profile(String name, Set<Topic> subscribedTopics) {
		this.name = name;
		this.id = ThreadLocalRandom.current().nextLong();
		this.subscribedTopics = subscribedTopics;
	}
	
	/**
	 * Get the username of the profile.
	 * @return the name of the user
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Get a unique id of the user.
	 * @return the user's id
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * Get all the subscribed topics.
	 * @return a copy of all the subscribed topics
	 */
	public Set<Topic> getSubscribedTopics() {
		HashSet<Topic> copyTopics = new HashSet<Topic>();
		copyTopics.addAll(subscribedTopics);
		return copyTopics;
	}
	
	/**
	 * Unsubscribe the user from a topic.
	 * @param topic the topic 
	 * @throws NoSuchElementException if the user wasn't subscribed to the topic
	 */
	public void unsubscribeFromTopic(Topic topic) throws NoSuchElementException {
		subscribedTopics.remove(topic);
	}
	
	/**
	 * Subscribe to a new topic
	 * @param topic the topic
	 */
	public void subscribeToTopic(Topic topic) {
		subscribedTopics.add(topic);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Profile other = (Profile) obj;
		return id == other.id;
	}
	
}

