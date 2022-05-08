package eventDeliverySystem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;

import eventDeliverySystem.client.Consumer;
import eventDeliverySystem.client.Publisher;
import eventDeliverySystem.datastructures.Post;
import eventDeliverySystem.datastructures.Topic;
import eventDeliverySystem.filesystem.Profile;
import eventDeliverySystem.filesystem.ProfileFileSystem;


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

	private static final String LOCALHOST   = "127.0.0.1";
	private static final int    BROKER_PORT = 29470;

	// TODO: remove
	@SuppressWarnings("javadoc")
	public static User forAlex(boolean existing, boolean local) throws IOException {
		String ip   = LOCALHOST;
		int    port = BROKER_PORT;
		Path   path = Path.of("C:\\Users\\alexm\\projects\\Java\\MessagingApp\\users\\");

		long   id   = 4355701369199818913L;
		String name = "alex";

		if (existing) {
			if (local)
				return loadExistingLocal(path, id);
			return loadExisting(ip, port, path, id);
		}

		if (local)
			return createNewLocal(path, name);

		return createNew(ip, port, path, name);
	}

	// TODO: remove
	@SuppressWarnings("javadoc")
	public static User forDimits(boolean existing, boolean local) throws IOException {
		String ip   = LOCALHOST;
		int    port = BROKER_PORT;
		Path   path = Path.of("C:\\Users\\alexm\\projects\\Java\\MessagingApp\\users\\");

		long   id   = -2731238523881095591L;
		String name = "dimtis";

		if (existing) {
			if (local)
				return loadExistingLocal(path, id);
			return loadExisting(ip, port, path, id);
		}

		if (local)
			return createNewLocal(path, name);

		return createNew(ip, port, path, name);
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
			this.consumer = new Consumer(serverIP, port, new UserSub());
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
		consumer.setTopics(new HashSet<>(currentProfile.getTopics()));
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
		consumer.setTopics(new HashSet<>(currentProfile.getTopics()));
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
	 * Attempts to push a new Topic. If this succeeds,
	 * {@link #listenForTopic(String)} is called.
	 *
	 * @param topicName the name of the Topic to create
	 *
	 * @return {@code true} if it was successfully created, {@code false} otherwise
	 *
	 * @throws IOException if an I/O error occurs while interacting with the file
	 *                     system
	 *
	 * @throw IllegalArgumentException if a Topic with the same name already exists
	 */
	public boolean createTopic(String topicName) throws IOException {
		LG.sout("User#createTopic(%s)", topicName);
		LG.in();
		boolean success = publisher.createTopic(topicName);
		LG.sout("success=%s", success);
		if (success)
			listenForTopic(topicName);

		LG.out();
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
		LG.sout("User#pull from Topic '%s'", topicName);
		LG.in();
		List<Post> newPosts = consumer.pull(topicName); // sorted from latest to earliest
		Collections.reverse(newPosts);
		LG.sout("newPosts=%s", newPosts);
		currentProfile.updateTopic(topicName, newPosts);

		for (Post post : newPosts) {
			LG.sout("Saving Post '%s'", post);
			profileFileSystem.savePost(post, topicName);
		}
		LG.out();
	}

	/**
	 * Registers a new Topic for which new Posts will be pulled and adds it to the
	 * Profile and file system. The pulled topics will be added to the Profile and
	 * saved to the file system.
	 *
	 * @param topicName the name of the Topic to listen for
	 *
	 * @throws IOException              if an I/O error occurs while interacting
	 *                                  with the file system
	 * @throws NullPointerException     if topic == null
	 * @throws IllegalArgumentException if a Topic with the same name already exists
	 */
	public void listenForTopic(String topicName) throws IOException {
		consumer.listenForTopic(topicName);
		currentProfile.addTopic(topicName);
		profileFileSystem.createTopic(topicName);
	}

	// temporary stuff because we don't have android

	// TODO: remove
	private CrappyUserUI.UserUISub uuisub;

	// TODO: remove
	public void setUserUISub(CrappyUserUI.UserUISub uuisub) {
		this.uuisub = uuisub;
	}

	// end of temporary stuff because we don't have android

	/**
	 * An object that can be used to notify this User about an event for a Topic.
	 *
	 * @author Alex Mandelias
	 */
	public class UserSub {

		private UserSub() {}

		/**
		 * Notifies this User about an event for a Topic.
		 *
		 * @param topicName the name of the Topic
		 */
		public void notify(String topicName) {
			User.this.currentProfile.markUnread(topicName);
			LG.sout("YOU HAVE A NEW MESSAGE AT '%s'", topicName);

			// TODO: remove
			if (uuisub != null)
				uuisub.notify(topicName);
		}
	}

	// ==================== LOCAL VERSIONS OF METHODS ====================

	// TODO: remove all local methods once done

	/**
	 * Retrieve the user's data and the saved posts. Calling any of the non-local
	 * methods on this User will result in a NullPointerException.
	 *
	 * @param profilesRootDirectory the root directory of all the Profiles in the
	 *                              file system
	 * @param profileId             the id of the existing profile
	 *
	 * @return the new User that only works with the local methods
	 *
	 * @throws IOException if an I/O error occurs while interacting with the file
	 *                     system or while establishing connection to the server
	 */
	public static User loadExistingLocal(Path profilesRootDirectory, long profileId)
	        throws IOException {
		User user = new User(profilesRootDirectory);
		user.switchToExistingProfileLocal(profileId);
		return user;
	}

	/**
	 * Creates a new User in the file system and returns a new User object. Calling
	 * any of the non-local methods on this User will result in a
	 * NullPointerException.
	 *
	 * @param profilesRootDirectory the root directory of all the Profiles in the
	 *                              file system
	 * @param name                  the name of the new Profile
	 *
	 * @return the new User that only works with the local methods
	 *
	 * @throws IOException if an I/O error occurs while interacting with the file
	 *                     system or while establishing connection to the server
	 */
	public static User createNewLocal(Path profilesRootDirectory, String name) throws IOException {
		User user = new User(profilesRootDirectory);
		user.switchToNewProfileLocal(name);
		return user;
	}

	private User(Path profilesRootDirectory) {
		this.profileFileSystem = new ProfileFileSystem(profilesRootDirectory);
		this.publisher = null;
		this.consumer = null;
	}

	/**
	 * Switches this User to manage a new Profile.
	 *
	 * @param profileName the name of the new Profile
	 *
	 * @throws IOException if an I/O error occurs while creating the new Profile
	 */
	public void switchToNewProfileLocal(String profileName) throws IOException {
		currentProfile = profileFileSystem.createNewProfile(profileName);
	}

	/**
	 * Switches this User to manage an existing.
	 *
	 * @param profileId the id of an existing Profile
	 *
	 * @throws IOException if an I/O error occurs while loading the existing Profile
	 */
	public void switchToExistingProfileLocal(long profileId) throws IOException {
		currentProfile = profileFileSystem.loadProfile(profileId);
	}

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
	 * Attempts to create a new Topic. If this operation succeeds, it is written to
	 * the file system.
	 *
	 * @param topicName the name of the Topic to create (not used in local)
	 *
	 * @return {@code true} if it was successfully created, {@code false} otherwise
	 */
	public boolean createTopicLocal(String topicName) {
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
			profileFileSystem.savePost(post, topicName);
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
		profileFileSystem.createTopic(topic.getName());
	}

}