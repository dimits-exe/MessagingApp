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

		public TopicData(String name) {
			this.topic = new Topic(name);
			this.pointer = 0L;
			this.broker = null;
		}
	}

	private static class TopicManager implements AutoCloseable {

		private final Map<String, TopicData> tdMap = new HashMap<>();

		public Topic get(String topicName) {
			return tdMap.get(topicName).topic;
		}

		public List<Post> fetch(String topicName) {
			TopicData  td = tdMap.get(topicName);
			List<Post> newPosts;

			if (td.pointer == 0) // 0 is default value of pointer
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

		public void update(String topicName, Socket broker) {
			TopicData td = tdMap.get(topicName);
			if (td == null) {
				td = new TopicData(topicName);
				tdMap.put(topicName, td);
			}
			td.broker = broker;
		}

		@Override
		public void close() throws IOException {
			for (TopicData td : tdMap.values())
				td.broker.close();
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
		topicManager = new TopicManager();

		for (Topic topic : topics)
			listenForTopic(topic.getName());
	}

	/**
	 * Returns all Posts which have not been previously pulled, from a Topic.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @return a List with all the Posts not yet pulled
	 */
	public List<Post> pull(String topicName) {
		return topicManager.fetch(topicName);
	}
	
	/**
	 * Register a new topic for the Consumer to monitor and
	 * automatically download new posts from.
	 * @param topicName the name of the new topic
	 */
	public void listenForTopic(String topicName) {
		Socket  socket = null;
		while (true) {
			ConnectionInfo ci = topicCIManager.getConnectionInfoForTopic(topicName);

			try {
				socket = new Socket(ci.getAddress(), ci.getPort());
			} catch (IOException e) {
				// invalidate and ask again for topicName;
				topicCIManager.invalidate(topicName);
				continue;
			}

			topicManager.update(topicName, socket);
			break;
		}

		try {
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.flush();
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

			Topic topic = topicManager.get(topicName);
			oos.writeObject(new Message(INITIALISE_CONSUMER, topic.getToken()));

			new PullThread(ois, topic).start();

		} catch (IOException e1) {
			throw new UncheckedIOException(e1); // 'socket' closes at closeImpl()
		}
	}
}
