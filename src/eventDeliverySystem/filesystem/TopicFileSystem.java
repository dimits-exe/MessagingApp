package eventDeliverySystem.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import eventDeliverySystem.datastructures.Post;
import eventDeliverySystem.datastructures.PostInfo;
import eventDeliverySystem.datastructures.Topic;

/**
 * Manages Topics that are saved in directories in the file system.
 *
 * @author Alex Mandelias
 */
public class TopicFileSystem {

	private static final Pattern pattern = Pattern
	        .compile("(?<postId>\\-?\\d+)\\-(?<posterId>\\-?\\d+)\\.(?<extension>.*)");
	private static final String  format  = "%d-%d.%s";

	private static final String HEAD                 = "HEAD";
	private static final String TOPIC_META_EXTENSION = ".meta";

	private final Path topicsRootDirectory;

	/**
	 * Constructs a new Topic File System for a given root directory.
	 *
	 * @param topicsRootDirectory the root directory of the new file system whose
	 *                            sub-directories correspond to different Topics
	 */
	public TopicFileSystem(Path topicsRootDirectory) {
		this.topicsRootDirectory = topicsRootDirectory;
	}

	/**
	 * Returns the all the Topic names found in the root directory.
	 *
	 * @return a collection of all the Topic names found
	 *
	 * @throws IOException if an I/O error occurs when opening the root directory
	 */
	public Stream<String> getTopicNames() throws IOException {
		return Files.list(topicsRootDirectory).filter(path -> Files.isDirectory(path))
		        .map(path -> path.getFileName().toString());
	}

	/**
	 * Creates a new empty Topic in the file system.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @throws IOException if a topic with that name already exists in this file
	 *                     system
	 */
	public void createTopic(String topicName) throws IOException {
		final Path topicDirectory = resolveRoot(topicName);
		Files.createDirectory(topicDirectory);

		final Path head = getHead(topicName);
		TopicFileSystem.create(head);
	}

	/**
	 * Deletes a {@link Topic} from the local File System. This operation is not
	 * atomic, meaning that if an Exception is thrown the local File System may
	 * still contain some of the Topic's files, leaving it in an ambiguous state.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @throws IOException if the topic or a post within a topic could not be
	 *                     deleted.
	 */
	public void deleteTopic(String topicName) throws IOException {
		final Path topicDirectory = resolveRoot(topicName);
		for (Iterator<Path> iter = Files.list(topicDirectory).iterator(); iter.hasNext();)
			Files.delete(iter.next());
	}

	/**
	 * Adds a new {@link Post} to an existing {@link Topic}.
	 *
	 * @param post      the new Post
	 * @param topicName the topic's name
	 *
	 * @throws IOException if the Post couldn't be written to the File System
	 */
	public void writePost(Post post, String topicName) throws IOException {
		final Path fileForPost = writePost0(post, topicName);
		writePointerForPost(post, topicName);
		updateHeadForPost(fileForPost, topicName);
	}

	/**
	 * Reads a {@link Topic} from the File System and returns it.
	 *
	 * @param topicName the topic's name
	 *
	 * @return a Topic object containing the Posts read from the File System
	 *
	 * @throws IOException if the Topic couldn't be loaded.
	 */
	public Topic readTopic(String topicName) throws IOException {
		final List<Post> loadedPosts = new LinkedList<>();

		final Path firstPost = getFirstPost(topicName);
		for (Path postFile = firstPost; postFile != null; postFile = getNextFile(postFile,
		        topicName)) {
			final String   filename   = postFile.getFileName().toString();
			final PostInfo postInfo   = TopicFileSystem.getPostInfoFromFileName(filename);
			final Post     loadedPost = TopicFileSystem.readPost(postInfo, postFile);
			loadedPosts.add(loadedPost);
		}

		return new Topic(topicName, loadedPosts);
	}

	/**
	 * Reads all Topics from the File System and returns them.
	 *
	 * @return a Collection including all the Posts loaded from the File System
	 *
	 * @throws IOException if the Topics couldn't be loaded.
	 */
	public Collection<Topic> readAllTopics() throws IOException {
		final Set<Topic> topics = new HashSet<>();

		for (Iterator<String> iter = getTopicNames().iterator(); iter.hasNext();)
			topics.add(readTopic(iter.next()));

		return topics;
	}

	// ==================== HELPERS FOR PATH ====================

	private Path resolveRoot(String topicName) {
		return TopicFileSystem.resolve(topicsRootDirectory, topicName);
	}

	private static Path resolve(Path directory, String filename) {
		return directory.resolve(filename);
	}

	// ==================== HELPERS FOR SAVE POST ====================

	private Path writePost0(Post post, String topicName)
	        throws FileNotFoundException, IOException {
		final String fileName = TopicFileSystem.getFileNameFromPostInfo(post.getPostInfo());

		final Path topicDirectory = resolveRoot(topicName);
		final Path pathForPost    = TopicFileSystem.resolve(topicDirectory, fileName);

		TopicFileSystem.create(pathForPost);

		final byte[] data = post.getData();
		TopicFileSystem.write(pathForPost, data);

		return pathForPost;
	}

	private void writePointerForPost(Post post, String topicName)
	        throws FileNotFoundException, IOException {
		final String fileName = TopicFileSystem.getFileNameFromPostInfo(post.getPostInfo());

		final Path   topicDirectory    = resolveRoot(topicName);
		final String metaFileName      = fileName + TopicFileSystem.TOPIC_META_EXTENSION;
		final Path   pointerToNextPost = TopicFileSystem.resolve(topicDirectory,
		        metaFileName);
		TopicFileSystem.create(pointerToNextPost);

		final Path   head         = getHead(topicName);
		final byte[] headContents = TopicFileSystem.read(head);
		TopicFileSystem.write(pointerToNextPost, headContents);
	}

	private void updateHeadForPost(Path fileForPost, String topicName) throws IOException {
		final Path   head            = getHead(topicName);
		final byte[] newHeadContents = fileForPost.getFileName().toString().getBytes();
		TopicFileSystem.write(head, newHeadContents);
	}

	private Path getHead(String topicName) {
		final Path topicDirectory = resolveRoot(topicName);
		return TopicFileSystem.resolve(topicDirectory, TopicFileSystem.HEAD);
	}

	// ==================== HELPERS FOR LOAD POSTS FOR TOPIC ====================

	// returns null if topic has no posts
	private Path getFirstPost(String topicName) throws FileNotFoundException, IOException {
		final Path   head         = getHead(topicName);
		final byte[] headContents = TopicFileSystem.read(head);

		if (headContents.length == 0)
			return null;

		final Path   topicDirectory = resolveRoot(topicName);
		final String firstPostFile  = new String(headContents);
		return TopicFileSystem.resolve(topicDirectory, firstPostFile);
	}

	// returns null if there is no next post
	private Path getNextFile(Path postFile, String topicName) throws IOException {
		final Path pointerToNextPost = postFile.resolve(TopicFileSystem.TOPIC_META_EXTENSION);

		final byte[] pointerToNextPostContents = TopicFileSystem.read(pointerToNextPost);

		if (pointerToNextPostContents.length == 0)
			return null;

		final Path   topicDirectory = resolveRoot(topicName);
		final String fileName       = new String(pointerToNextPostContents);
		return TopicFileSystem.resolve(topicDirectory, fileName);
	}

	private static Post readPost(PostInfo postInfo, Path postFile)
	        throws FileNotFoundException, IOException {
		final byte[] data = TopicFileSystem.read(postFile);
		return new Post(data, postInfo);
	}

	// ==================== READ/WRITE ====================

	private static void create(Path pathForPost) throws IOException {
		Files.createFile(pathForPost);
	}

	private static byte[] read(Path head) throws IOException {
		return Files.readAllBytes(head);
	}

	private static void write(Path pointerToNextPost, byte[] data) throws IOException {
		Files.write(pointerToNextPost, data);
	}

	// ==================== POST INFO ====================

	private static String getFileNameFromPostInfo(PostInfo postInfo) {
		final long   postId        = postInfo.getId();
		final String posterId      = postInfo.getPosterName();
		final String fileExtension = postInfo.getFileExtension();

		return String.format(TopicFileSystem.format, postId, posterId, fileExtension);
	}

	private static PostInfo getPostInfoFromFileName(String fileName) {
		final Matcher m = TopicFileSystem.pattern.matcher(fileName);

		if (!m.find())
			throw new RuntimeException("Bad filename: " + fileName);

		final long   postId        = Long.parseLong(m.group("postId"));
		final String posterId      = m.group("posterId");
		final String fileExtension = m.group("extension");

		return new PostInfo(posterId, fileExtension, postId);
	}
}
