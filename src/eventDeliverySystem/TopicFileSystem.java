package eventDeliverySystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO
 *
 * @author Alex Mandelias
 */
public class TopicFileSystem {

	private static final Pattern pattern = Pattern
	        .compile("(<?postId>\\-?\\d+)\\-(<?posterid>\\d+)\\.(<?extension>.*)");
	private static final String  format  = "%d-%d.%s";

	private static final String HEAD = "HEAD";
	private static final String META = ".meta";


	private final Path topicsDirectory;

	/**
	 * Constructs a new Post File System for a given root directory.
	 *
	 * @param topicsDirectory the root directory that contains the sub-directories
	 *                        for the different Topics
	 */
	public TopicFileSystem(Path topicsDirectory) {
		this.topicsDirectory = topicsDirectory;
	}

	private Path getTopicDirectory(String topicName) {
		String userDir = topicsDirectory.toString();
		return Path.of(userDir, topicName);
	}

	private static File getFileInDirectory(Path directory, String filename) {
		String dir = directory.toAbsolutePath().toString();
		return Path.of(dir, filename).toFile();
	}

	/**
	 * TODO
	 *
	 * @param topicName
	 *
	 * @throws IOException
	 */
	public void addTopic(String topicName) throws IOException {
		File topicDirectory = getTopicDirectory(topicName).toFile();

		if (!topicDirectory.mkdir()) {
			throw new IOException(
			        String.format("Directory for Topic %s already exists", topicName));
		}

		File head = getHead(topicName);
		create(head);
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
		File fileForPost = writePost(post, topicName);
		writePointerForPost(post, topicName);
		updateHeadForPost(fileForPost, topicName);
	}

	/**
	 * TODO
	 *
	 * @param topicName
	 *
	 * @return
	 *
	 * @throws IOException
	 */
	public List<Post> loadPostsForTopic(String topicName) throws IOException {
		List<Post> loadedPosts = new LinkedList<>();

		File firstPost = getFirstPost(topicName);
		for (File postFile = firstPost; postFile != null; postFile = getNextFile(postFile)) {
			PostInfo postInfo   = getPostInfoFromFileName(postFile.getName());
			Post     loadedPost = readPost(postInfo, postFile);
			loadedPosts.add(loadedPost);
		}

		return loadedPosts;
	}

	/**
	 * TODO
	 *
	 * @param emptyUser
	 *
	 * @throws IOException
	 */
	public void loadTopicsForProfile(Profile emptyUser) throws IOException {
		File[]     topicDirectories = topicsDirectory.toFile().listFiles(File::isDirectory);
		Set<Topic> loadedTopics     = new HashSet<>();

		for (File topicDirectory : topicDirectories) {
			String     topicName = topicDirectory.getName();
			List<Post> posts     = loadPostsForTopic(topicName);
			loadedTopics.add(new Topic(topicName, posts));
		}

		for (Topic topic : loadedTopics)
			emptyUser.addTopic(topic);
	}

	// ==================== HELPERS FOR SAVE POST ====================

	private File writePost(Post post, String topicName)
	        throws FileNotFoundException, IOException {
		String fileName = getFileNameFromPostInfo(post.getPostInfo());

		Path topicDirectory = getTopicDirectory(topicName);
		File fileForPost    = getFileInDirectory(topicDirectory, fileName);

		create(fileForPost);

		byte[] data = post.getData();
		write(fileForPost, data);

		return fileForPost;
	}

	private void writePointerForPost(Post post, String topicName)
	        throws FileNotFoundException, IOException {
		String fileName = getFileNameFromPostInfo(post.getPostInfo());

		Path topicDirectory    = getTopicDirectory(topicName);
		File pointerToNextPost = getFileInDirectory(topicDirectory, fileName + META);
		create(pointerToNextPost);

		File   head         = getHead(topicName);
		byte[] headContents = read(head);
		write(pointerToNextPost, headContents);
	}

	private void updateHeadForPost(File fileForPost, String topicName) throws IOException {
		File   head            = getHead(topicName);
		byte[] newHeadContents = fileForPost.getName().getBytes();
		write(head, newHeadContents);
	}

	private File getHead(String topicName) {
		Path topicDirectory = getTopicDirectory(topicName);
		return getFileInDirectory(topicDirectory, HEAD);
	}

	// ==================== HELPERS FOR LOAD POSTS FOR TOPIC ====================

	// returns null if topic has no posts
	private File getFirstPost(String topicName) throws FileNotFoundException, IOException {
		File   head         = getHead(topicName);
		byte[] headContents = read(head);

		if (headContents.length == 0)
			return null;

		Path   topicDirectory = getTopicDirectory(topicName);
		String firstPostFile  = new String(headContents);
		return getFileInDirectory(topicDirectory, firstPostFile);
	}

	// returns null if there is no next post
	private static File getNextFile(File postFile) throws IOException {
		File   pointerToNextPost     = new File(postFile.toPath() + META);
		byte[] pointerToNextPostData = read(pointerToNextPost);

		if (pointerToNextPostData.length == 0)
			return null;

		String nextPost = new String(pointerToNextPostData);
		return new File(nextPost);
	}

	private static Post readPost(PostInfo postInfo, File postFile)
	        throws FileNotFoundException, IOException {
		byte[] data = read(postFile);
		return new Post(data, postInfo);
	}

	// ==================== READ/WRITE ====================

	private static void create(File file) throws IOException {
		if (!file.createNewFile())
			throw new IOException(String.format("File %s already exists", file));
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

	// ==================== POST INFO ====================

	private static String getFileNameFromPostInfo(PostInfo postInfo) {
		long   postId        = postInfo.getId();
		long   posterId      = postInfo.getPosterId();
		String fileExtension = postInfo.getFileExtension();

		return String.format(format, postId, posterId, fileExtension);
	}

	private static PostInfo getPostInfoFromFileName(String fileName) {
		Matcher m = pattern.matcher(fileName);

		if (!m.find())
			throw new RuntimeException("Bad filename: " + fileName);

		long   postId        = Long.parseLong(m.group("postId"));
		long   posterId      = Long.parseLong(m.group("posterId"));
		String fileExtension = m.group("extension");

		return new PostInfo(posterId, fileExtension, postId);
	}

}
