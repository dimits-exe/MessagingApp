package eventDeliverySystem.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import eventDeliverySystem.datastructures.Post;
import eventDeliverySystem.datastructures.Topic;

/**
 * Manages Profiles that are saved in directories in the file system.
 *
 * @author Alex Mandelias
 */
public class ProfileFileSystem {

	private final Path                         profilesRootDirectory;
	private final Map<String, TopicFileSystem> topicFileSystemMap;

	private String currentProfileName;

	/**
	 * Creates a new Profile File System for the specified root directory.
	 *
	 * @param profilesRootDirectory the root directory of the new file system whose
	 *                              sub-directories correspond to different Profiles
	 *
	 * @throws IOException if an I/O error occurs when initialising the file system
	 */
	public ProfileFileSystem(Path profilesRootDirectory) throws IOException {
		this.profilesRootDirectory = profilesRootDirectory;
		topicFileSystemMap = new HashMap<>();

		getProfileNames().forEach(profileName -> {
			final TopicFileSystem tfs = new TopicFileSystem(getTopicsDirectory(profileName));
			topicFileSystemMap.put(profileName, tfs);
		});
	}

	/**
	 * Returns the all the Profile names found in the root directory.
	 *
	 * @return a collection of all the Profile names found
	 *
	 * @throws IOException if an I/O error occurs when opening the root directory
	 */
	public Stream<String> getProfileNames() throws IOException {
		return Files.list(profilesRootDirectory).filter(path -> Files.isDirectory(path))
		        .map(path -> path.getFileName().toString());
	}

	/**
	 * Creates a new, empty, Profile in this File System.
	 *
	 * @param profileName the name of the new Profile
	 *
	 * @return the new Profile
	 *
	 * @throws IOException if an I/O error occurs while creating the new Profile
	 */
	public Profile createNewProfile(String profileName) throws IOException {
		Path topicsDirectory = getTopicsDirectory(profileName);
		Files.createDirectory(topicsDirectory);

		topicFileSystemMap.put(profileName, new TopicFileSystem(topicsDirectory));

		changeProfile(profileName);

		return new Profile(profileName);
	}

	/**
	 * Reads a Profile from this File System and returns it as a Profile object.
	 * After this method returns, this file system will operate on the new Profile.
	 *
	 * @param profileName the id of the Profile to read
	 *
	 * @return the Profile read
	 *
	 * @throws IOException if an I/O error occurs while interacting with the file
	 *                     system
	 */
	public Profile loadProfile(String profileName) throws IOException {
		changeProfile(profileName);

		final Profile profile = new Profile(profileName);

		final TopicFileSystem tfs = getTopicFileSystemForCurrentUser();
		for (final Topic topic : tfs.readAllTopics())
			profile.addTopic(topic);

		return profile;
	}

	/**
	 * Creates a new Topic for the current Profile.
	 *
	 * @param topicName the name of the new Topic
	 *
	 * @throws IOException if an I/O error occurs while interacting with the file
	 *                     system
	 */
	public void createTopic(String topicName) throws IOException {
		getTopicFileSystemForCurrentUser().createTopic(topicName);
	}

	/**
	 * Saves a Post in the file system for the current Profile.
	 *
	 * @param post      the Post to save
	 * @param topicName the name of the Topic in which to save
	 *
	 * @throws IOException if an I/O error occurs while interacting with the file
	 *                     system
	 */
	public void savePost(Post post, String topicName) throws IOException {
		getTopicFileSystemForCurrentUser().writePost(post, topicName);
	}

	// ==================== PRIVATE METHODS ====================

	private void changeProfile(String profileName) throws NoSuchElementException {
		if (!topicFileSystemMap.containsKey(profileName))
			throw new NoSuchElementException("Profile " + profileName + " does not exist");

		currentProfileName = profileName;
	}

	private TopicFileSystem getTopicFileSystemForCurrentUser() {
		return topicFileSystemMap.get(currentProfileName);
	}

	private Path getTopicsDirectory(String profileName) {
		return profilesRootDirectory.resolve(profileName);
	}
}
