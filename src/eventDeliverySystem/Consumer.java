package eventDeliverySystem;

import static eventDeliverySystem.Message.MessageType.INITIALISE_CONSUMER;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A client-side process which is responsible for listening for a set of Topics
 * and pulling Posts from them by connecting to a remote server.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 *
 * @see Broker
 */
class Consumer extends ClientNode {

	private static class TopicData {

		private Topic  topic;
		private long   pointer;
		private Socket broker;

		public TopicData(Topic topic) {
			this.topic = topic;
			pointer = topic.getLastPostId();
			broker = null;
		}
	}

	private static class TopicManager implements AutoCloseable {

		private final Map<String, TopicData> tdMap = new HashMap<>();

		public TopicManager(Set<Topic> topics) {
			for (Topic topic : topics) {
				add(topic);
			}
		}

		public List<Post> fetch(String topicName) {
			if (!tdMap.containsKey(topicName))
				throw new NoSuchElementException("No Topic with name " + topicName + " found");

			TopicData  td = tdMap.get(topicName);
			List<Post> newPosts;

			if (td.pointer == -1) // see Topic#getLastPostId() and TopicData#TopicData(Topic)
				newPosts = td.topic.getAllPosts();
			else
				newPosts = td.topic.getPostsSince(td.pointer);

			// update topic pointer if there is at least one new Post
			int newPostCount = newPosts.size();
			if (newPostCount > 1) {
				long lastPostId = newPosts.get(newPostCount - 1).getPostInfo().getId();
				tdMap.get(topicName).pointer = lastPostId;
			}

			return newPosts;
		}

		public void addSocket(Topic topic, Socket broker) {
			String topicName = topic.getName();
			if (tdMap.containsKey(topicName))
				throw new IllegalArgumentException(
				        "Topic with name " + topicName + " already exists");

			add(topic);

			TopicData td = tdMap.get(topic.getName());
			td.broker = broker;
		}

		private void add(Topic topic) {
			tdMap.put(topic.getName(), new TopicData(topic));
		}

		@Override
		public void close() throws IOException {
			for (TopicData td : tdMap.values())
				td.broker.close();

			tdMap.clear();
		}
	}

	private final TopicManager topicManager;

	// TODO: inform user when a new Post for a Topic arrives

	/**
	 * Constructs a Consumer that will connect to a specific default broker.
	 *
	 * @param serverIP   the IP of the default broker, interpreted by
	 *                   {@link InetAddress#getByName(String)}.
	 * @param serverPort the port of the default broker
	 * @param topics     the Topics for which this Consumer listens, which may
	 *                   already contain some Posts
	 *
	 * @throws UnknownHostException if no IP address for the host could be found, or
	 *                              if a scope_id was specified for a global IPv6
	 *                              address while resolving the defaultServerIP.
	 * @throws IOException          if an I/O error occurs while establishing
	 *                              connection to the server
	 */
	public Consumer(String serverIP, int serverPort, Set<Topic> topics)
	        throws IOException {
		this(InetAddress.getByName(serverIP), serverPort, topics);
	}

	/**
	 * Constructs a Consumer that will connect to a specific default broker.
	 *
	 * @param serverIP   the IP of the default broker, interpreted by
	 *                   {@link InetAddress#getByAddress(byte[])}.
	 * @param serverPort the port of the default broker
	 * @param topics     the Topics for which this Consumer listens, which may
	 *                   already contain some Posts
	 *
	 * @throws UnknownHostException if IP address is of illegal length
	 * @throws IOException          if an I/O error occurs while establishing
	 *                              connection to the server
	 */
	public Consumer(byte[] serverIP, int serverPort, Set<Topic> topics)
	        throws IOException {
		this(InetAddress.getByAddress(serverIP), serverPort, topics);
	}

	/**
	 * Constructs a Consumer that will connect to a specific default broker.
	 *
	 * @param ip     the InetAddress of the default broker
	 * @param port   the port of the default broker
	 * @param topics the Topics for which this Consumer listens, which may already
	 *               contain some Posts
	 *
	 * @throws IOException if an I/O error occurs while establishing connection to
	 *                     the server
	 */
	private Consumer(InetAddress ip, int port, Set<Topic> topics) throws IOException {
		super(ip, port);
		topicManager = new TopicManager(topics);
		topics.forEach(this::listenForTopic);
	}

	/**
	 * Changes the Topics that this Consumer listens to. All connections regarding
	 * the previous Topics are closed and new ones are established.
	 *
	 * @param newTopics the new Topics to listen for
	 *
	 * @throws IOException if an I/O error occurs while closing existing connections
	 */
	public void setTopics(Set<Topic> newTopics) throws IOException {
		topicManager.close();
		newTopics.forEach(this::listenForTopic);
	}

	/**
	 * Returns all Posts which have not been previously pulled, from a Topic.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @return a List with all the Posts not yet pulled
	 *
	 * @throws NoSuchElementException if no Topic with the given name exists
	 */
	public List<Post> pull(String topicName) {
		return topicManager.fetch(topicName);
	}

	/**
	 * Registers a new Topic for this Consumer to automatically pull new Posts from.
	 *
	 * @param topic the Topic to pull from
	 */
	public void listenForTopic(Topic topic) {
		String topicName = topic.getName();
		Socket socket    = null;

		while (true) {
			ConnectionInfo ci = topicCIManager.getConnectionInfoForTopic(topicName);

			try {
				socket = new Socket(ci.getAddress(), ci.getPort());
			} catch (IOException e) {
				// invalidate and ask again for topicName
				topicCIManager.invalidate(topicName);
				continue;
			}

			topicManager.addSocket(topic, socket);
			break;
		}

		try {
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream()); // socket can't be null
			oos.flush();
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

			oos.writeObject(new Message(INITIALISE_CONSUMER, topic.getToken()));

			new PullThread(ois, topic).start();

		} catch (IOException e1) {
			throw new UncheckedIOException(e1); // 'socket' closes at closeImpl()
		}
	}
}
