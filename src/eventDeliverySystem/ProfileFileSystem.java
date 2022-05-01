package eventDeliverySystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TODO
 *
 * @author Alex Mandelias
 */
class ProfileFileSystem {

	private static final String PROFILE_META = "profile.meta";

	private final Map<Long, TopicFileSystem> topicFileSystemMap;

	private final Path profilesRootDirectory;
	private long       currentProfileId;

	/**
	 * TODO
	 *
	 * @param profilesRootDirectory
	 */
	public ProfileFileSystem(Path profilesRootDirectory) {
		this.profilesRootDirectory = profilesRootDirectory;
		topicFileSystemMap = new HashMap<>();
	}

	/**
	 * TODO
	 *
	 * @param name
	 *
	 * @return
	 *
	 * @throws IOException
	 */
	public Profile createNewProfile(String name) throws IOException {
		long id;
		Path profileDirectory;
		do {
			id = ThreadLocalRandom.current().nextLong();
			profileDirectory = getProfileDirectory(id);
		} while (!profileDirectory.toFile().mkdir());

		File   profileMeta     = getFileInDirectory(profileDirectory, PROFILE_META);
		profileMeta.createNewFile();
		byte[] profileNameData = name.getBytes();
		write(profileMeta, profileNameData);

		return new Profile(name, id);
	}

	/**
	 * TODO
	 *
	 * @param profileId
	 *
	 * @return
	 *
	 * @throws IOException
	 */
	public Profile loadProfile(long profileId) throws IOException {
		Path profileDirectory = getProfileDirectory(profileId);
		topicFileSystemMap.put(profileId, new TopicFileSystem(profileDirectory));

		File    profileMeta     = getFileInDirectory(profileDirectory, PROFILE_META);
		byte[]  profileNameData = read(profileMeta);
		String  userName        = new String(profileNameData);
		Profile profile         = new Profile(userName, profileId);
		topicFileSystemMap.get(profile.getId()).loadTopicsForProfile(profile);
		return profile;
	}

	/**
	 * TODO
	 *
	 * @param newProfile
	 */
	public void changeProfile(Profile newProfile) {
		currentProfileId = newProfile.getId();
	}

	/**
	 * TODO
	 *
	 * @param topicName
	 *
	 * @throws IOException
	 */
	public void addTopic(String topicName) throws IOException {
		getTopicFileSystemForCurrentUser().addTopic(topicName);
	}

	/**
	 * TODO
	 *
	 * @param post
	 * @param topicName
	 *
	 * @throws IOException
	 */
	public void savePostToFileSystem(Post post, String topicName) throws IOException {
		getTopicFileSystemForCurrentUser().savePostToFileSystem(post, topicName);
	}

	private TopicFileSystem getTopicFileSystemForCurrentUser() {
		return topicFileSystemMap.get(currentProfileId);
	}

	private Path getProfileDirectory(long profileId) {
		String profileDir = profilesRootDirectory.toAbsolutePath().toString();
		return Path.of(profileDir, String.valueOf(profileId));
	}

	private static File getFileInDirectory(Path directory, String filename) {
		String dir = directory.toAbsolutePath().toString();
		return Path.of(dir, filename).toFile();
	}

	private static byte[] read(File file) throws IOException {
		try (FileInputStream fis = new FileInputStream(file)) {
			return fis.readAllBytes();
		} catch (FileNotFoundException e) {
			System.err.printf("File %s could not be found", file);
		}

		return null;
	}

	private static void write(File file, byte[] data) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(data);
		} catch (FileNotFoundException e) {
			System.err.printf("File %s could not be found", file);
		}
	}

}