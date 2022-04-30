package eventDeliverySystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A class that manages the actions of the user by communicating with the server
 * and retrieving / committing posts to the file system.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
public class User {

	private static final Pattern pattern = Pattern
	        .compile("(<?postId>\\-?\\d+)\\-(<?posterid>\\d+)\\.(<?extension>.*)");
	private static final String  format  = "%d-%d.%s";

	private static final String HEAD = "HEAD";
	private static final String META = ".meta";

	private Publisher  publisher;
	private Consumer   consumer;
	private final Path usersDir;
	public Profile     user;     // TODO: make private, it is public for UserTest

	// TODO: remove
	public static User forAlex() throws IOException {
		return new User(null, -1,
		        Path.of("C:\\Users\\alexm\\projects\\Java\\MessagingApp\\users\\"), 1);
	}

	// TODO: remove
	public static User forDimits() throws IOException {
		throw new RuntimeException("User::44, go write the method lmao");
	}

	/**
	 * Retrieve the user's data and the saved posts, establish connection to the
	 * server and prepare to receive and send posts.
	 *
	 * @param serverIP the IP of the server
	 * @param port     the port of the server
	 * @param usersDir the base directory of the users
	 * @param userId   the id of the profile of this user. The directory
	 *                 'usersDir\\userId' contains all the Topics and saved posts.
	 *
	 * @throws IOException if a connection can't be established with the server or
	 *                     if the file system can't be reached
	 */
	public User(InetAddress serverIP, int port, Path usersDir, long userId)
	        throws IOException {
		this.usersDir = usersDir;

		try {
			user = loadEmptyUser(userId);
		} catch (IOException e) {
			// TODO: remove throw, add:
			// user = createNewEmptyUser(userId);
			throw new IOException("Could not create user " + userId, e);
		}

		try {
			loadTopicsForUser(user);
		} catch (IOException e) {
			throw new IOException("Could not load data for user " + userId, e);
		}

		try {
			this.publisher = new Publisher(serverIP, port);
			this.consumer = new Consumer(serverIP, port,
			        new HashSet<>(user.getTopics().values()));
		} catch(IOException ioe) {
			throw new IOException("Could not establish connection with server", ioe);
		}
	}

	/*
	@foff
	TOOD: Methods to implement:

	Pubilsher#push
	Publisher#createTopic
	Consumer#pull
	Consumer#listenForTopic
	@fon
	 */

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

	// TODO: add doc below and potentially fix implementation

	public void pull(String topicName) throws IOException {
		List<Post> posts = consumer.pull(topicName);
		user.updateTopic(topicName, posts);

		for (Post post : posts) {
			savePostToFileSystem(post, topicName);
		}
	}

	public void createTopic(String topicName) throws IOException {
		publisher.createTopic(topicName);
		user.addTopic(topicName);
		addTopic(topicName);
	}

	public void listenForTopic(String topicName) {
		consumer.listenForTopic(topicName);
		user.addTopic(topicName);
	}

	// ==================== LOCAL VERSIONS OF METHODS ====================

	public void pullLocal(String topicName, List<Post> posts) throws IOException {
		user.updateTopic(topicName, posts);

		for (Post post : posts) {
			savePostToFileSystem(post, topicName);
		}
	}

	public void createTopicLocal(String topicName) throws IOException {
		user.addTopic(topicName);
		addTopic(topicName);
	}

	// TODO: add remaining local versions of methods

	// ============================== PRIVATE METHODS ==============================

	// ==================== CREATE / LOAD UESR ====================

	// TODO: add createNewUser

	private Profile loadEmptyUser(long userId) throws IOException {
		File   profileMeta  = getFileInDirectory(getUserDirectory(userId), META);
		byte[] userNameData = read(profileMeta);
		String userName     = new String(userNameData);
		return new Profile(userName, userId);
	}

	private void loadTopicsForUser(Profile emptyUser) throws IOException {
		File[] topicDirectories = getUserDirectory().toFile().listFiles(File::isDirectory);
		Set<Topic> loadedTopics = new HashSet<>();

		for (File topicDirectory : topicDirectories) {
			String     topicName = topicDirectory.getName();
			List<Post> posts     = loadPostsForTopic(topicName);
			loadedTopics.add(new Topic(topicName, posts));
		}

		for (Topic topic : loadedTopics)
			emptyUser.addTopic(topic);
	}

	// ==================== PATH HELPER METHODS ====================

	private Path getUserDirectory() {
		return getUserDirectory(user.getId());
	}

	private Path getUserDirectory(long userId) {
		String userDir = usersDir.toAbsolutePath().toString();
		return Path.of(userDir, String.valueOf(userId));
	}

	private Path getTopicDirectory(String topicName) {
		String userDir = getUserDirectory().toString();
		return Path.of(userDir, topicName);
	}

	private static File getFileInDirectory(Path directory, String filename) {
		String dir = directory.toAbsolutePath().toString();
		return Path.of(dir, filename).toFile();
	}

	// ==================== CREATE TOPIC ====================

	private void addTopic(String topicName) throws IOException {
		File topicDirectory = getTopicDirectory(topicName).toFile();

		if (!topicDirectory.mkdir()) {
			throw new IOException(
			        String.format("Directory for Topic %s already exists", topicName));
		}

		File head = getHead(topicName);
		create(head);
	}

	// ==================== SAVE POST ====================

	private void savePostToFileSystem(Post post, String topicName) throws IOException {
		File fileForPost = writePost(post, topicName);
		writePointerForPost(post, topicName);
		updateHeadForPost(fileForPost, topicName);
	}

	private File writePost(Post post, String topicName) throws FileNotFoundException, IOException {
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

	// ==================== LOAD POSTS FOR TOPIC ====================

	private List<Post> loadPostsForTopic(String topicName) throws IOException {
		List<Post> loadedPosts = new LinkedList<>();

		File firstPost = getFirstPost(topicName);
		for (File postFile = firstPost; postFile != null; postFile = getNextFile(postFile)) {
			PostInfo postInfo   = getPostInfoFromFileName(postFile.getName());
			Post     loadedPost = readPost(postInfo, postFile);
			loadedPosts.add(loadedPost);
		}

		return loadedPosts;
	}

	// returns null if topic has no posts
	private File getFirstPost(String topicName) throws FileNotFoundException, IOException {
		File head = getHead(topicName);
		byte[] headContents = read(head);

		if (headContents.length == 0)
			return null;

		Path   topicDirectory  = getTopicDirectory(topicName);
		String firstPostFile = new String(headContents);
		return getFileInDirectory(topicDirectory, firstPostFile);
	}

	// returns null if there is no next post
	private static File getNextFile(File postFile) throws IOException {
		File   pointerToNextPost = new File(postFile.toPath() + META);
		byte[] pointerToNextPostData = read(pointerToNextPost);

		if (pointerToNextPostData.length == 0)
			return null;

		String nextPost = new String(pointerToNextPostData);
		return new File(nextPost);
	}

	private static Post readPost(PostInfo postInfo, File postFile) throws FileNotFoundException, IOException {
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
