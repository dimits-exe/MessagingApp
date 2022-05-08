package eventDeliverySystem;

import static eventDeliverySystem.Message.MessageType.INITIALISE_CONSUMER;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import eventDeliverySystem.User.UserSub;

/**
 * A client-side process which is responsible for listening for a set of Topics
 * and pulling Posts from them by connecting to a remote server.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 *
 * @see Broker
 */
class Consumer extends ClientNode implements Subscriber {
	private final UserSub      usersub;
	private final TopicManager topicManager;

	/**
	 * Constructs a Consumer that will connect to a specific default broker.
	 *
	 * @param serverIP   the IP of the default broker, interpreted by
	 *                   {@link InetAddress#getByName(String)}.
	 * @param serverPort the port of the default broker
	 * @param usersub    the UserSub object that will be notified when data arrives
	 *
	 * @throws UnknownHostException if no IP address for the host could be found, or
	 *                              if a scope_id was specified for a global IPv6
	 *                              address while resolving the defaultServerIP.
	 * @throws IOException          if an I/O error occurs while establishing
	 *                              connection to the server
	 */
	public Consumer(String serverIP, int serverPort, UserSub usersub)
	        throws IOException {
		this(InetAddress.getByName(serverIP), serverPort, usersub);
	}

	/**
	 * Constructs a Consumer that will connect to a specific default broker.
	 *
	 * @param serverIP   the IP of the default broker, interpreted by
	 *                   {@link InetAddress#getByAddress(byte[])}.
	 * @param serverPort the port of the default broker
	 * @param usersub    the UserSub object that will be notified when data arrives
	 *
	 * @throws UnknownHostException if IP address is of illegal length
	 * @throws IOException          if an I/O error occurs while establishing
	 *                              connection to the server
	 */
	public Consumer(byte[] serverIP, int serverPort, UserSub usersub)
	        throws IOException {
		this(InetAddress.getByAddress(serverIP), serverPort, usersub);
	}

	/**
	 * Constructs a Consumer that will connect to a specific default broker.
	 *
	 * @param ip      the InetAddress of the default broker
	 * @param port    the port of the default broker
	 * @param usersub the UserSub object that will be notified when data arrives
	 *
	 * @throws IOException if an I/O error occurs while establishing connection to
	 *                     the server
	 */
	private Consumer(InetAddress ip, int port, UserSub usersub) throws IOException {
		super(ip, port);
		topicManager = new TopicManager();
		this.usersub = usersub;
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

		for (final Topic topic : newTopics) {
			final String topicName = topic.getName();
			listenForTopic(topicName);

			// TODO: contemplate life choices
			final List<Post> posts    = topic.getAllPosts();

			final long idOfLast;
			if (posts.size() != 0)
				idOfLast = posts.get(0).getPostInfo().getId();
			else
				idOfLast = Topic.FETCH_ALL_POSTS;
			Collections.reverse(posts);
			topicManager.tdMap.get(topicName).topic.post(posts);
			topicManager.tdMap.get(topicName).pointer = idOfLast;
		}
	}

	/**
	 * Returns all Posts from a Topic which have not been previously pulled.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @return a List with all the Posts not yet pulled, sorted from latest to
	 *         earliest
	 *
	 * @throws NoSuchElementException if no Topic with the given name exists
	 */
	public List<Post> pull(String topicName) {
		return topicManager.fetch(topicName);
	}

	/**
	 * Registers a new Topic for this Consumer to continuously fetch new Posts from.
	 *
	 * @param topicName the name of the Topic to fetch from
	 *
	 * @throws IllegalArgumentException if this Consumer already listens to a Topic
	 *                                  with the same name
	 */
	@SuppressWarnings("resource") // 'socket' closes at closeImpl()
	public void listenForTopic(String topicName) {
		LG.sout("listenForTopic=%s", topicName);
		LG.in();
		final Topic topic  = new Topic(topicName);
		topic.subscribe(this);
		Socket      socket = null;

		while (true) {
			final ConnectionInfo ci = topicCIManager.getConnectionInfoForTopic(topicName);

			try {
				socket = new Socket(ci.getAddress(), ci.getPort());
			} catch (final IOException e) {
				// invalidate and ask again for topicName
				topicCIManager.invalidate(topicName);
				continue;
			}

			topicManager.addSocket(topic, socket);
			break;
		}

		try {
			if(socket == null)
				throw new RuntimeException("No connection could be established with the Broker");
			
			final ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream()); // socket can't be null
			oos.flush();
			final ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

			oos.writeObject(new Message(INITIALISE_CONSUMER, topic.getToken()));

			new PullThread(ois, topic).start();

		} catch (final IOException e1) {
			throw new UncheckedIOException(e1);
		}
		LG.out();
	}

	@Override
	public void notify(PostInfo postInfo, String topicName) {
		LG.sout("Consumer#notify(%s)", postInfo);
		// do nothing
	}

	@Override
	public void notify(Packet packet, String topicName) {
		LG.sout("Consumer#notify(%s)", packet);
		if (packet.isFinal())
			usersub.notify(topicName);
	}
	
	private static class TopicData {
		private final Topic topic;
		private long        pointer;
		private Socket      socket;

		public TopicData(Topic topic) {
			this.topic = topic;
			pointer = topic.getLastPostId();
			socket = null;
		}
	}

	private static class TopicManager implements AutoCloseable {

		private final Map<String, TopicData> tdMap = new HashMap<>();

		/**
		 * Returns all Posts from a Topic which have not been previously fetched.
		 *
		 * @param topicName the name of the Topic
		 *
		 * @return a List with all the Posts not yet fetched, sorted from latest to
		 *         earliest
		 *
		 * @throws NoSuchElementException if no Topic with the given name exists
		 */
		public List<Post> fetch(String topicName) {
			LG.sout("Consumer#fetch(%s)", topicName);
			LG.in();
			if (!tdMap.containsKey(topicName))
				throw new NoSuchElementException("No Topic with name " + topicName + " found");

			final TopicData td = tdMap.get(topicName);
			List<Post>      newPosts;

			LG.sout("td.pointer=%d", td.pointer);
			if (td.pointer == Topic.FETCH_ALL_POSTS) // see Topic#getLastPostId() and TopicData#TopicData(Topic)
				newPosts = td.topic.getAllPosts();
			else
				newPosts = td.topic.getPostsSince(td.pointer);

			// update topic pointer if there is at least one new Post
			final int newPostCount = newPosts.size();
			LG.sout("newPostCount=%d", newPostCount);
			if (newPostCount > 0) {
				final long lastPostId = newPosts.get(0).getPostInfo().getId();
				td.pointer = lastPostId;
			}

			td.topic.clear();

			LG.out();
			return newPosts;
		}

		/**
		 * Adds a Topic to this Manager and registers its socket from where to fetch.
		 *
		 * @param topic  the Topic
		 * @param socket the socket from where it will fetch
		 *
		 * @throws IllegalArgumentException if this Manager already has a socket for a
		 *                                  Topic with the same name.
		 */
		public void addSocket(Topic topic, Socket socket) {
			add(topic);
			final TopicData td = tdMap.get(topic.getName());
			td.socket = socket;
		}

		private void add(Topic topic) {
			final String topicName = topic.getName();
			if (tdMap.containsKey(topicName))
				throw new IllegalArgumentException(
				        "Topic with name " + topicName + " already exists");

			tdMap.put(topicName, new TopicData(topic));
		}

		@Override
		public void close() throws IOException {
			for (final TopicData td : tdMap.values())
				td.socket.close();

			tdMap.clear();
		}
	}
}
