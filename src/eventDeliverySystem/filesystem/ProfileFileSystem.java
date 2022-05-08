package eventDeliverySystem.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eventDeliverySystem.datastructures.Post;
import eventDeliverySystem.datastructures.Topic;

/**
 * Manages Profiles that are saved in directories in the file system.
 *
 * @author Alex Mandelias
 */
public class ProfileFileSystem {

	private static final String PROFILE_META = "profile.meta";

	private final Path                       profilesRootDirectory;
	private final Map<Long, TopicFileSystem> topicFileSystemMap;

	private long currentProfileId;

	/**
	 * Creates a new Profile File System for the specified root directory.
	 *
	 * @param profilesRootDirectory the root directory of the new file system whose
	 *                              sub-directories correspond to different Profiles
	 */
	public ProfileFileSystem(Path profilesRootDirectory) {
		this.profilesRootDirectory = profilesRootDirectory;
		topicFileSystemMap = new HashMap<>();

		for (final String id : getProfileIDs()) {
			final long            profileId = Long.parseLong(id);
			final TopicFileSystem tfs       = new TopicFileSystem(getTopicsDirectory(profileId));
			topicFileSystemMap.put(profileId, tfs);
		}
	}

	/**
	 * Returns the all the Profile IDs found in the root directory.
	 *
	 * @return a collection of all the Profile IDs found
	 */
	public Collection<String> getProfileIDs() {
		final File[]         profileDirectories = profilesRootDirectory.toFile()
		        .listFiles(File::isDirectory);
		final Stream<String> nameStream         = Stream.of(profileDirectories).map(File::getName);
		return new HashSet<>(nameStream.collect(Collectors.toList()));
	}

	/**
	 * Creates a new, empty, Profile in this File System.
	 *
	 * @param name the name of the new Profile
	 *
	 * @return the new Profile
	 *
	 * @throws IOException if an I/O error occurs while interacting with the file
	 *                     system
	 */
	public Profile createNewProfile(String name) throws IOException {
		long profileId;
		Path topicsDirectory;
		do {
			profileId = ThreadLocalRandom.current().nextLong();
			topicsDirectory = getTopicsDirectory(profileId);
		} while (!topicsDirectory.toFile().mkdir());

		topicFileSystemMap.put(profileId, new TopicFileSystem(topicsDirectory));

		changeProfile(profileId);

		final File profileMeta = getProfileMeta();
		profileMeta.createNewFile();
		final byte[] profileNameData = name.getBytes();
		ProfileFileSystem.write(profileMeta, profileNameData);

		return new Profile(name, currentProfileId);
	}

	/**
	 * Reads a Profile from this File System and returns it as a Profile object.
	 * After this method returns, this file system will operate on the new Profile.
	 *
	 * @param profileId the id of the Profile to read
	 *
	 * @return the Profile read
	 *
	 * @throws IOException if an I/O error occurs while interacting with the file
	 *                     system
	 */
	public Profile loadProfile(long profileId) throws IOException {
		changeProfile(profileId);

		final File    profileMeta     = getProfileMeta();
		final byte[]  profileNameData = ProfileFileSystem.read(profileMeta);
		final String  name            = new String(profileNameData);
		final Profile profile         = new Profile(name, currentProfileId);

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

	private void changeProfile(long profileId) throws NoSuchElementException {
		if (!topicFileSystemMap.containsKey(profileId))
			throw new NoSuchElementException("No profile with id " + profileId + " exists");

		currentProfileId = profileId;
	}

	private TopicFileSystem getTopicFileSystemForCurrentUser() {
		return topicFileSystemMap.get(currentProfileId);
	}

	private Path getTopicsDirectory(long profileId) {
		final String root = profilesRootDirectory.toAbsolutePath().toString();
		return Path.of(root, String.valueOf(profileId));
	}

	private File getProfileMeta() {
		final Path topicsDirectory = getTopicsDirectory(currentProfileId);
		return Path.of(topicsDirectory.toString(), ProfileFileSystem.PROFILE_META).toFile();
	}

	// ==================== READ/WRITE ====================

	private static byte[] read(File file) throws IOException {
		try (FileInputStream fis = new FileInputStream(file)) {
			return fis.readAllBytes();
		}
	}

	private static void write(File file, byte[] data) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(data);
		} catch (final FileNotFoundException e) {
			System.err.printf("File %s could not be found", file);
		}
	}

}
