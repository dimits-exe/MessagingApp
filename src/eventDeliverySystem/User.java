package eventDeliverySystem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * A class that manages the actions of the user by communicating with the server
 * and retrieving / committing posts to the file system.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
public class User {

	private final ProfileFileSystem profileFileSystem;
	private Profile                 currentProfile;

	private final Publisher publisher;
	private final Consumer  consumer;

	// TODO: remove
	@SuppressWarnings("javadoc")
	public static User forAlex() throws IOException {
		return loadExisting("", -1,
		        Path.of("C:\\Users\\alexm\\projects\\Java\\MessagingApp\\users\\"), 1);
	}

	// TODO: remove
	@SuppressWarnings("javadoc")
	public static User forDimits() throws IOException {
		throw new RuntimeException("User::47, go write the method lmao");
		// return loadExisting("", -1, Path.of("C:\\Your\\Path\\Here\\"), 2);
	}

	/**
	 * Retrieve the user's data and the saved posts, establish connection to the
	 * server and prepare to receive and send posts.
	 *
	 * @param serverIP              the IP of the server
	 * @param serverPort            the port of the server
	 * @param profilesRootDirectory the root directory of all the Profiles in the
	 *                              file system
	 * @param profileId             the id of the existing profile
	 *
	 * @return the new User
	 *
	 * @throws IOException if an I/O error occurs while interacting with the file
	 *                     system or while establishing connection to the server
	 */
	public static User loadExisting(String serverIP, int serverPort, Path profilesRootDirectory,
	        long profileId) throws IOException {
		User user = new User(serverIP, serverPort, profilesRootDirectory);
		user.switchToExistingProfile(profileId);
		return user;
	}

	/**
	 * Creates a new User in the file system and returns a new User object.
	 *
	 * @param serverIP              the IP of the server
	 * @param serverPort            the port of the server
	 * @param profilesRootDirectory the root directory of all the Profiles in the
	 *                              file system
	 * @param name                  the name of the new Profile
	 *
	 * @return the new User
	 *
	 * @throws IOException if an I/O error occurs while interacting with the file
	 *                     system or while establishing connection to the server
	 */
	public static User createNew(String serverIP, int serverPort, Path profilesRootDirectory,
	        String name) throws IOException {
		User user = new User(serverIP, serverPort, profilesRootDirectory);
		user.switchToNewProfile(name);
		return user;
	}

	private User(String serverIP, int port, Path profilesRootDirectory)
	        throws IOException {
		this.profileFileSystem = new ProfileFileSystem(profilesRootDirectory);

		try {
			this.publisher = new Publisher(serverIP, port);
			this.consumer = new Consumer(serverIP, port, Collections.emptySet());
		} catch (IOException e) {
			throw new IOException("Could not establish connection with server", e);
		}
	}

	/**
	 * Returns this User's current Profile.
	 *
	 * @return the current Profile
	 */
	public Profile getCurrentProfile() {
		return currentProfile;
	}

	/**
	 * Switches this User to manage a new Profile.
	 *
	 * @param profileName the name of the new Profile
	 *
	 * @throws IOException if an I/O error occurs while creating the new Profile
	 */
	public void switchToNewProfile(String profileName) throws IOException {
		currentProfile = profileFileSystem.createNewProfile(profileName);
		consumer.setTopics(new HashSet<>(currentProfile.getTopics().values()));
	}

	/**
	 * Switches this User to manage an existing.
	 *
	 * @param profileId the id of an existing Profile
	 *
	 * @throws IOException if an I/O error occurs while loading the existing Profile
	 */
	public void switchToExistingProfile(long profileId) throws IOException {
		currentProfile = profileFileSystem.loadProfile(profileId);
		consumer.setTopics(new HashSet<>(currentProfile.getTopics().values()));
	}

	/**
	 * Posts a Post to a Topic.
	 *
	 * @param post      the Post to post
	 * @param topicName the name of the Topic to which to post
	 *
	 * @see Publisher#push(Post, String)
	 */
	public void post(Post post, String topicName) {
		publisher.push(post, topicName);
	}

	/**
	 * Attempts to create a new Topic and add it to the Profile. If this operation
	 * succeeds, the new Topic is pushed. If it is not pushed successfully, the
	 * Profile's Topic is deleted.
	 *
	 * @param topicName the name of the Topic to create
	 *
	 * @return {@code true} if it was successfully created, {@code false} otherwise
	 *
	 * @throw IllegalArgumentException if a Topic with the same name already exists
	 */
	public boolean createTopic(String topicName) {
		currentProfile.addTopic(topicName);

		boolean success = publisher.createTopic(topicName);
		if (!success)
			currentProfile.removeTopic(topicName);

		return success;
	}

	/**
	 * Pulls all new Posts from a Topic, adds them to the Profile and saves them to
	 * the file system. Posts that have already been pulled are not pulled again.
	 *
	 * @param topicName the name of the Topic from which to pull
	 *
	 * @throws IOException            if an I/O Error occurs while writing the new
	 *                                Posts to the file system
	 * @throws NoSuchElementException if no Topic with the given name exists
	 */
	public void pull(String topicName) throws IOException {
		List<Post> newPosts = consumer.pull(topicName);
		currentProfile.updateTopic(topicName, newPosts);

		for (Post post : newPosts) {
			profileFileSystem.savePostToFileSystem(post, topicName);
		}
	}

	/**
	 * Registers a new Topic for which new Posts will be pulled. The pulled topics
	 * will be added to the Profile and saved to the file system.
	 *
	 * @param topic the Topic to listen for
	 *
	 * @throws IOException              if an I/O error occurs while interacting
	 *                                  with the file system
	 * @throws NullPointerException     if topic == null
	 * @throws IllegalArgumentException if a Topic with the same name already exists
	 */
	public void listenForTopic(Topic topic) throws IOException {
		consumer.listenForTopic(topic);
		currentProfile.addTopic(topic);
		profileFileSystem.addTopic(topic.getName());
	}

	// ==================== LOCAL VERSIONS OF METHODS ====================

	// TODO: remove all local methods once done

	/**
	 * Does nothing.
	 *
	 * @param post      the Post to post
	 * @param topicName the name of the Topic to which to post
	 *
	 * @see Publisher#push(Post, String)
	 */
	@SuppressWarnings({ "static-method", "unused" })
	public void postLocal(Post post, String topicName) {
		;
	}

	/**
	 * Attempts to create a new Topic and add it to the Profile. If this operation
	 * succeeds, it is written to the file system.
	 *
	 * @param topicName the name of the Topic to create
	 *
	 * @return {@code true} if it was successfully created, {@code false} otherwise
	 *
	 * @throws IOException if an I/O Error occurs while writing the newly created
	 *                     Topic to the file system
	 */
	public boolean createTopicLocal(String topicName) throws IOException {
		currentProfile.addTopic(topicName);
		profileFileSystem.addTopic(topicName);
		return true;
	}

	/**
	 * Adds some Posts them to the Profile and saves them to the file system.
	 *
	 * @param topicName the name of the Topic from which to pull
	 * @param newPosts  the posts that would be pulled from the Consumer
	 *
	 * @throws IOException            if an I/O Error occurs while writing the new
	 *                                Posts to the file system
	 * @throws NoSuchElementException if no Topic with the given name exists
	 */
	public void pullLocal(String topicName, List<Post> newPosts) throws IOException {
		currentProfile.updateTopic(topicName, newPosts);

		for (Post post : newPosts) {
			profileFileSystem.savePostToFileSystem(post, topicName);
		}
	}

	/**
	 * Registers a new Topic for which new Posts will be pulled and creates that
	 * topic in the file system. The pulled topics will be added to the Profile and
	 * saved to the file system.
	 *
	 * @param topic the Topic to listen for
	 *
	 * @throws IOException              if an I/O error occurs while interacting
	 *                                  with the file system
	 * @throws NullPointerException     if topic == null
	 * @throws IllegalArgumentException if a Topic with the same name already exists
	 */
	public void listenForTopicLocal(Topic topic) throws IOException {
		currentProfile.addTopic(topic);
		profileFileSystem.addTopic(topic.getName());
	}
}
