package com.example.messagingapp.eventDeliverySystem.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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

import com.example.messagingapp.eventDeliverySystem.datastructures.Post;
import com.example.messagingapp.eventDeliverySystem.datastructures.PostInfo;
import com.example.messagingapp.eventDeliverySystem.datastructures.Topic;

/**
 * Manages Topics that are saved in directories in the file system.
 *
 * @author Alex Mandelias
 */
public class TopicFileSystem implements Serializable {

	private static final Pattern PATTERN = Pattern
	        .compile("(?<postId>\\-?\\d+)\\-(?<posterName>\\-?\\d+)\\.(?<extension>.*)");
	private static final String  FORMAT  = "%d-%s.%s";

	private static final String HEAD                 = "HEAD";
	private static final String TOPIC_META_EXTENSION = ".meta";

	private final File topicsRootDirectory;

	/**
	 * Constructs a new Topic File System for a given root directory.
	 *
	 * @param topicsRootDirectory the root directory of the new file system whose
	 *                            sub-directories correspond to different Topics
	 */
	public TopicFileSystem(Path topicsRootDirectory) {
		this.topicsRootDirectory = topicsRootDirectory.toFile();
	}

	/**
	 * Returns the all the Topic names found in the root directory.
	 *
	 * @return a collection of all the Topic names found
	 *
	 * @throws FileSystemException if an I/O error occurs while interacting with the
	 *                             file system
	 */
	public Stream<String> getTopicNames() throws FileSystemException {
		try {
			return Files.list(getRoot())
			        .filter(Files::isDirectory)
			        .map(path -> path.getFileName().toString());
		} catch (IOException e) {
			throw new FileSystemException(getRoot(), e);
		}
	}

	/**
	 * Creates a new empty Topic in the file system.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @throws FileSystemException if a topic with that name already exists in this
	 *                             file system
	 */
	public void createTopic(String topicName) throws FileSystemException {
		final Path topicDirectory = resolveRoot(topicName);
		try {
			Files.createDirectory(topicDirectory);
		} catch (IOException e) {
			throw new FileSystemException(topicDirectory, e);
		}

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
	 * @throws FileSystemException if an I/O error occurs while interacting with the
	 *                             file system
	 */
	public void deleteTopic(String topicName) throws FileSystemException {
		final Path topicDirectory = resolveRoot(topicName);

		Path currentPath = topicDirectory;
		try (Stream<Path> directoryStream = Files.list(currentPath)) {
			for (Iterator<Path> iter = directoryStream.iterator(); iter.hasNext();) {
				currentPath = iter.next();
				Files.delete(currentPath);
			}
		} catch (IOException e) {
			throw new FileSystemException(currentPath, e);
		}
	}

	/**
	 * Adds a new {@link Post} to an existing {@link Topic}.
	 *
	 * @param post      the new Post
	 * @param topicName the topic's name
	 *
	 * @throws FileSystemException if an I/O error occurs while interacting with the
	 *                             file system
	 */
	public void writePost(Post post, String topicName) throws FileSystemException {
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
	 * @throws FileSystemException if an I/O error occurs while interacting with the
	 *                             file system
	 */
	public Topic readTopic(String topicName) throws FileSystemException {
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
	 * @throws FileSystemException if an I/O error occurs while interacting with the
	 *                             file system
	 */
	public Collection<Topic> readAllTopics() throws FileSystemException {
		final Set<Topic> topics = new HashSet<>();

		for (Iterator<String> iter = getTopicNames().iterator(); iter.hasNext();)
			topics.add(readTopic(iter.next()));

		return topics;
	}

	// ==================== HELPERS FOR PATH ====================

	private Path getRoot() {
		return topicsRootDirectory.toPath();
	}

	private Path resolveRoot(String topicName) {
		return TopicFileSystem.resolve(getRoot(), topicName);
	}

	private static Path resolve(Path directory, String filename) {
		return directory.resolve(filename);
	}

	// ==================== HELPERS FOR SAVE POST ====================

	private Path writePost0(Post post, String topicName) throws FileSystemException {
		final String fileName = TopicFileSystem.getFileNameFromPostInfo(post.getPostInfo());

		final Path topicDirectory = resolveRoot(topicName);
		final Path pathForPost    = TopicFileSystem.resolve(topicDirectory, fileName);

		TopicFileSystem.create(pathForPost);

		final byte[] data = post.getData();
		TopicFileSystem.write(pathForPost, data);

		return pathForPost;
	}

	private void writePointerForPost(Post post, String topicName) throws FileSystemException {
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

	private void updateHeadForPost(Path fileForPost, String topicName) throws FileSystemException {
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
	private Path getFirstPost(String topicName) throws FileSystemException {
		final Path   head         = getHead(topicName);
		final byte[] headContents = TopicFileSystem.read(head);

		if (headContents.length == 0)
			return null;

		final Path   topicDirectory = resolveRoot(topicName);
		final String firstPostFile  = new String(headContents);
		return TopicFileSystem.resolve(topicDirectory, firstPostFile);
	}

	// returns null if there is no next post
	private Path getNextFile(Path postFile, String topicName) throws FileSystemException {
		final Path pointerToNextPost = postFile.resolve(TopicFileSystem.TOPIC_META_EXTENSION);

		final byte[] pointerToNextPostContents = TopicFileSystem.read(pointerToNextPost);

		if (pointerToNextPostContents.length == 0)
			return null;

		final Path   topicDirectory = resolveRoot(topicName);
		final String fileName       = new String(pointerToNextPostContents);
		return TopicFileSystem.resolve(topicDirectory, fileName);
	}

	private static Post readPost(PostInfo postInfo, Path postFile) throws FileSystemException {
		final byte[] data = TopicFileSystem.read(postFile);
		return new Post(data, postInfo);
	}

	// ==================== READ/WRITE ====================

	private static void create(Path pathForPost) throws FileSystemException {
		try {
		Files.createFile(pathForPost);
		} catch (IOException e) {
			throw new FileSystemException(pathForPost, e);
		}
	}

	private static byte[] read(Path head) throws FileSystemException {
		try {
			return Files.readAllBytes(head);
		} catch (IOException e) {
			throw new FileSystemException(head, e);
		}
	}

	private static void write(Path pointerToNextPost, byte[] data) throws FileSystemException {
		try {
			Files.write(pointerToNextPost, data);
		} catch (IOException e) {
			throw new FileSystemException(pointerToNextPost, e);
		}
	}

	// ==================== POST INFO ====================

	private static String getFileNameFromPostInfo(PostInfo postInfo) {
		final long   postId        = postInfo.getId();
		final String posterId      = postInfo.getPosterName();
		final String fileExtension = postInfo.getFileExtension();

		return String.format(TopicFileSystem.FORMAT, postId, posterId, fileExtension);
	}

	private static PostInfo getPostInfoFromFileName(String fileName) {
		final Matcher m = TopicFileSystem.PATTERN.matcher(fileName);

		if (m.matches()) {
			final long   postId        = Long.parseLong(m.group("postId"));
			final String posterId      = m.group("posterName");
			final String fileExtension = m.group("extension");

			return new PostInfo(posterId, fileExtension, postId);
		}

		throw new IllegalArgumentException("Bad filename: " + fileName);
	}
}
