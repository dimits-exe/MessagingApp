package eventDeliverySystem.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
	 */
	public Collection<String> getTopicNames() {
		final File[]         topicDirectories = topicsRootDirectory.toFile()
		        .listFiles(File::isDirectory);
		final Stream<String> nameStream       = Stream.of(topicDirectories).map(File::getName);
		return new HashSet<>(nameStream.collect(Collectors.toList()));
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
		final File topicDirectory = getTopicDirectory(topicName).toFile();

		if (!topicDirectory.mkdir())
			throw new IOException(
			        String.format("Directory for Topic %s already exists", topicName));

		final File head = getHead(topicName);
		TopicFileSystem.create(head);
	}

	/**
	 * Deletes a {@link Topic} from the local File System.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @throws IOException if the topic could not be deleted.
	 */
	public void deleteTopic(String topicName) throws IOException {
		// TODO: auto-generated method stub

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
		final File fileForPost = writePost0(post, topicName);
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

		final File firstPost = getFirstPost(topicName);
		for (File postFile = firstPost; postFile != null; postFile = getNextFile(postFile,
		        topicName)) {
			final PostInfo postInfo   = TopicFileSystem.getPostInfoFromFileName(postFile.getName());
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
		for (final String topicName : getTopicNames())
			topics.add(readTopic(topicName));

		return topics;
	}

	// ==================== HELPERS FOR PATH ====================

	private Path getTopicDirectory(String topicName) {
		final String userDir = topicsRootDirectory.toString();
		return new File(userDir + File.separator + topicName).toPath();
	}

	private static File getFileInDirectory(Path directory, String filename) {
		final String dir = directory.toAbsolutePath().toString();
		return new File(dir + File.separator + filename);
	}

	// ==================== HELPERS FOR SAVE POST ====================

	private File writePost0(Post post, String topicName)
	        throws FileNotFoundException, IOException {
		final String fileName = TopicFileSystem.getFileNameFromPostInfo(post.getPostInfo());

		final Path topicDirectory = getTopicDirectory(topicName);
		final File fileForPost    = TopicFileSystem.getFileInDirectory(topicDirectory, fileName);

		TopicFileSystem.create(fileForPost);

		final byte[] data = post.getData();
		TopicFileSystem.write(fileForPost, data);

		return fileForPost;
	}

	private void writePointerForPost(Post post, String topicName)
	        throws FileNotFoundException, IOException {
		final String fileName = TopicFileSystem.getFileNameFromPostInfo(post.getPostInfo());

		final Path topicDirectory    = getTopicDirectory(topicName);
		final File pointerToNextPost = TopicFileSystem.getFileInDirectory(topicDirectory,
		        fileName + TopicFileSystem.TOPIC_META_EXTENSION);
		TopicFileSystem.create(pointerToNextPost);

		final File   head         = getHead(topicName);
		final byte[] headContents = TopicFileSystem.read(head);
		TopicFileSystem.write(pointerToNextPost, headContents);
	}

	private void updateHeadForPost(File fileForPost, String topicName) throws IOException {
		final File   head            = getHead(topicName);
		final byte[] newHeadContents = fileForPost.getName().getBytes();
		TopicFileSystem.write(head, newHeadContents);
	}

	private File getHead(String topicName) {
		final Path topicDirectory = getTopicDirectory(topicName);
		return TopicFileSystem.getFileInDirectory(topicDirectory, TopicFileSystem.HEAD);
	}

	// ==================== HELPERS FOR LOAD POSTS FOR TOPIC ====================

	// returns null if topic has no posts
	private File getFirstPost(String topicName) throws FileNotFoundException, IOException {
		final File   head         = getHead(topicName);
		final byte[] headContents = TopicFileSystem.read(head);

		if (headContents.length == 0)
			return null;

		final Path   topicDirectory = getTopicDirectory(topicName);
		final String firstPostFile  = new String(headContents);
		return TopicFileSystem.getFileInDirectory(topicDirectory, firstPostFile);
	}

	// returns null if there is no next post
	private File getNextFile(File postFile, String topicName) throws IOException {
		final File   pointerToNextPost         = new File(
		        postFile.toPath() + TopicFileSystem.TOPIC_META_EXTENSION);
		final byte[] pointerToNextPostContents = TopicFileSystem.read(pointerToNextPost);

		if (pointerToNextPostContents.length == 0)
			return null;

		final Path   topicDirectory = getTopicDirectory(topicName);
		final String fileName       = new String(pointerToNextPostContents);
		return TopicFileSystem.getFileInDirectory(topicDirectory, fileName);
	}

	private static Post readPost(PostInfo postInfo, File postFile)
	        throws FileNotFoundException, IOException {
		final byte[] data = TopicFileSystem.read(postFile);
		return new Post(data, postInfo);
	}

	// ==================== READ/WRITE ====================

	private static void create(File file) throws IOException {
		if (!file.createNewFile())
			throw new IOException(String.format("File %s already exists", file));
	}

	private static byte[] read(File file) throws IOException {
		System.out.println(file);
		try (FileInputStream fis = new FileInputStream(file)) {
			List<Integer> bytes = new ArrayList<>();

			int nextByte;
			while ((nextByte = fis.read()) != -1) {
				bytes.add(nextByte);
			}

			byte[] data = new byte[bytes.size()];
			for (int i = 0; i < data.length; i++)
				data[i] = (byte) ((int) bytes.get(i));

			return data;
		}
	}

	private static void write(File file, byte[] data) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(data);
		}
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
