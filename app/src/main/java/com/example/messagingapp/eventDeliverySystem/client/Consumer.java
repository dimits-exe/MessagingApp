package com.example.messagingapp.eventDeliverySystem.client;

import static com.example.messagingapp.eventDeliverySystem.datastructures.Message.MessageType.INITIALISE_CONSUMER;

import com.example.messagingapp.eventDeliverySystem.ISubscriber;
import com.example.messagingapp.eventDeliverySystem.datastructures.ConnectionInfo;
import com.example.messagingapp.eventDeliverySystem.datastructures.Message;
import com.example.messagingapp.eventDeliverySystem.datastructures.Packet;
import com.example.messagingapp.eventDeliverySystem.datastructures.Post;
import com.example.messagingapp.eventDeliverySystem.datastructures.PostInfo;
import com.example.messagingapp.eventDeliverySystem.datastructures.Topic;
import com.example.messagingapp.eventDeliverySystem.server.Broker;
import com.example.messagingapp.eventDeliverySystem.server.ServerException;
import com.example.messagingapp.eventDeliverySystem.thread.PullThread;
import com.example.messagingapp.eventDeliverySystem.util.LG;
import com.example.messagingapp.eventDeliverySystem.util.Subscriber;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A client-side process which is responsible for listening for a set of Topics
 * and pulling Posts from them by connecting to a remote server.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 *
 * @see Broker
 */
public class Consumer extends ClientNode implements AutoCloseable, Subscriber, Serializable {

	private final ISubscriber  usersub;
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
	 */
	public Consumer(String serverIP, int serverPort, ISubscriber usersub) throws UnknownHostException {
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
	 */
	public Consumer(byte[] serverIP, int serverPort, ISubscriber usersub) throws UnknownHostException {
		this(InetAddress.getByAddress(serverIP), serverPort, usersub);
	}

	/**
	 * Constructs a Consumer that will connect to a specific default broker.
	 *
	 * @param ip      the InetAddress of the default broker
	 * @param port    the port of the default broker
	 * @param usersub the UserSub object that will be notified when data arrives
	 */
	private Consumer(InetAddress ip, int port, ISubscriber usersub) {
		super(ip, port);
		topicManager = new TopicManager();
		this.usersub = usersub;
	}

	@Override
	public void close() throws ServerException {
		topicManager.close();
	}

	/**
	 * Changes the Topics that this Consumer listens to. All connections regarding
	 * the previous Topics are closed and new ones are established.
	 *
	 * @param newTopics the new Topics to listen for
	 *
	 * @throws ServerException if an I/O error occurs while closing existing
	 *                         connections
	 */
	public void setTopics(Set<Topic> newTopics) throws ServerException {
		topicManager.close();

		for (final Topic topic : newTopics)
			listenForTopic(topic);
	}

	/**
	 * Returns all Posts from a Topic which have not been previously pulled.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @return a List with all the Posts not yet pulled, sorted from earliest to
	 *         latest
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
	 * @throws ServerException          if a connection to the server fails
	 * @throws IllegalArgumentException if this Consumer already listens to a Topic
	 *                                  with the same name
	 */
	public void listenForNewTopic(String topicName) throws ServerException {
		LG.sout("listenForNewTopic(%s)", topicName);
		listenForTopic(new Topic(topicName));
	}

	/**
	 * Registers an existing Topic for this Consumer to continuously fetch new Posts
	 * from.
	 *
	 * @param topic Topic to fetch from
	 *
	 * @throws ServerException          if a connection to the server fails
	 * @throws IllegalArgumentException if this Consumer already listens to a Topic
	 *                                  with the same name
	 */
	@SuppressWarnings("resource") // 'socket' closes at close()
	private void listenForTopic(Topic topic) throws ServerException {
		topic.subscribe(this);

		final Socket[] socket = {null};
		final String topicName = topic.getName();

		final ConnectionInfo ci = topicCIManager.getConnectionInfoForTopic(topicName);

		// run connection acquisition on different thread so we don't freeze up the main
		// android thread

		// create callable so we can receive any exceptions that may arise
		Callable<Object> socketThread = () -> {
			try{
				socket[0] = new Socket(ci.getAddress(), ci.getPort()); // 'socket' closes at close()
				topicManager.addSocket(topic, socket[0]);

				final ObjectOutputStream oos = new ObjectOutputStream(socket[0].getOutputStream());
				oos.flush();
				final ObjectInputStream ois = new ObjectInputStream(socket[0].getInputStream());

				oos.writeObject(new Message(INITIALISE_CONSUMER, topic.getToken()));

				new PullThread(ois, topic).start();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return new Object(); // return value ignored
		};

		try {
			Future<Object> task = Executors.newSingleThreadExecutor().submit(socketThread);
			task.get(5L, TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			throw new ServerException(new IOException(e)); // dont worry about it
		} catch (InterruptedException ie){
			throw new RuntimeException(ie);
		} catch (TimeoutException e) {
			throw new ServerException(new IOException("Server connection timed out"));
		}
	}

	@Override
	public synchronized void notify(PostInfo postInfo, String topicName) {
		LG.sout("Consumer#notify(%s, %s)", postInfo, topicName);
		// do nothing
	}

	@Override
	public synchronized void notify(Packet packet, String topicName) {
		LG.sout("Consumer#notify(%s, %s)", packet, topicName);
		if (packet.isFinal())
			usersub.notify(topicName);
	}

	private static class TopicManager implements AutoCloseable, Serializable {

		private static class TopicData implements Serializable {
			private final Topic topic;
			private long        pointer;

			// transient socket = resource will be leaked every time it's serialized
			private transient Socket socket;

			public TopicData(Topic topic) {
				this.topic = topic;
				pointer = topic.getLastPostId();
				socket = null;
			}
		}

		private final Map<String, TopicData> tdMap = new HashMap<>();

		/**
		 * Returns all Posts from a Topic which have not been previously fetched.
		 *
		 * @param topicName the name of the Topic
		 *
		 * @return a List with all the Posts not yet fetched, sorted from earliest to
		 *         latest
		 *
		 * @throws NoSuchElementException if no Topic with the given name exists
		 */
		public List<Post> fetch(String topicName) {
			LG.sout("Consumer#fetch(%s)", topicName);
			LG.in();
			if (!tdMap.containsKey(topicName))
				throw new NoSuchElementException("No Topic with name " + topicName + " found");

			final TopicData td = tdMap.get(topicName);

			assert td != null;
			LG.sout("td.pointer=%d", td.pointer);
			final List<Post> newPosts = td.topic.getPostsSince(td.pointer);

			LG.sout("newPosts.size()=%d", newPosts.size());
			td.pointer = td.topic.getLastPostId();

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
			LG.sout("Consumer#addSocket(%s, %s)", topic, socket);
			add(topic);
			Objects.requireNonNull(tdMap.get(topic.getName())).socket = socket;
		}

		private void add(Topic topic) {
			final String topicName = topic.getName();
			if (tdMap.containsKey(topicName))
				throw new IllegalArgumentException(
				        "Topic with name " + topicName + " already exists");

			tdMap.put(topicName, new TopicManager.TopicData(topic));
		}

		@Override
		public void close() throws ServerException {
			try {
				for (final TopicManager.TopicData td : tdMap.values())
					td.socket.close();
			} catch (IOException e) {
				throw new ServerException(e);
			}

			tdMap.clear();
		}
	}
}
