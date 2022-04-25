package eventDeliverySystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eventDeliverySystem.Topic.TopicToken;

/**
 * TODO: fix.
 * <p>
 * A remote component that forms the backbone of the EventDeliverySystem.
 * Brokers act as part of a distributed server that services Publishers and
 * Consumers.
 */
class Broker implements Runnable {

	private static final PortManager portManager = new PortManager();
	private static final int MAX_CONNECTIONS = 64;

	private final Set<ConnectionInfo> publisherInfo;
	private final Map<String, Set<Socket>> consumerConnectionsPerTopic;
	private ServerSocket              clientRequestSocket;

	private final Map<String, Topic> topicsByName;

	// === PLACEHOLDERS ===

	private final List<Socket> brokerConnections;
	private final Map<Topic, LinkedList<Post>> postsPerTopic;
	private final Map<Topic, LinkedList<Post>> postsBackup;

	private boolean isLeader;

	// === END PLACEHOLDERS ===

	private ServerSocket brokerRequestSocket;


	/**
	 * Create a new broker and add it to the already existing
	 * distributed server system.
	 * @param leaderIP the IP of the broker acting as the leader of the system
	 */
	public Broker(InetAddress leaderIP) {
		this();
		isLeader = false;
	}

	/**
	 * Create a new broker that will act as the Leader of the
	 * distributed server.
	 */
	public Broker() {
		//TODO: establish connection with broker
		this.publisherInfo = Collections.synchronizedSet(new HashSet<>());
		this.consumerConnectionsPerTopic = Collections.synchronizedMap(new HashMap<>());
		this.brokerConnections = Collections.synchronizedList(new LinkedList<>());
		this.postsPerTopic = Collections.synchronizedMap(new HashMap<>());
		this.postsBackup = Collections.synchronizedMap(new HashMap<>());
		this.topicsByName = Collections.synchronizedMap(new HashMap<>());
		isLeader = true;
	}

	@Override
	public void run() {
		try {
			clientRequestSocket = new ServerSocket(portManager.getNewAvailablePort(), MAX_CONNECTIONS);
			brokerRequestSocket = new ServerSocket(portManager.getNewAvailablePort(), MAX_CONNECTIONS);

			// Start handling client requests
			Runnable clientRequestThread = () -> {
				while (true) {
					try {
						// TODO: figure out how to close this one
						Socket socket = clientRequestSocket.accept();
						new ClientRequestHandler(socket).start();

					} catch (IOException e) {
						e.printStackTrace();
						System.exit(-1); // serious error when waiting, close broker
					}
				}
			};


			//TODO: properly implement
			Runnable brokerRequestThread = new Runnable() {

				@Override
				public void run() {
					while(true) {
						try {
							brokerConnections.add(brokerRequestSocket.accept());
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(-1); 	// serious error when waiting, close broker
						}
					}
				}

			}; //brokerRequestThread

			new Thread(clientRequestThread).start();
			new Thread(brokerRequestThread).start();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the relevant worker thread to satisfy the request according to the
	 * {@link Message.MessageType message's type}.
	 *
	 * @param message    the message to be handled
	 * @param connection the socket from where the message was sent
	 *
	 * @return the worker thread for the request
	 *
	 * @throws IOException if an error occurs while establishing an output stream to
	 *                     write back to the sender
	 */
	private Thread threadFactory(Message message, Socket connection) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());
		ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());

		// fuck variables in switch statements
		Topic topic;
		String topicName;

		switch(message.getType()) {
		case DATA_PACKET_SEND:
			topicName = (String) message.getValue();
			topic = topicsByName.get(topicName);
			return new PullThread(ois, topic);

		case INITIALISE_CONSUMER:
			TopicToken topicToken = (TopicToken) message.getValue();
			topicName = topicToken.getName();
			long idOfLast = topicToken.getLastId();

			// register current connection as listener for topic
			consumerConnectionsPerTopic.get(topicName).add(connection);

			// send existing topics that the consumer does not have
			topic = topicsByName.get(topicName);
			List<Post> postsToSend = topic.getPostsSince(idOfLast);
			return new PushThread(oos, postsToSend, true); // keep consumer's thread alive

		case PUBLISHER_DISCOVERY_REQUEST: // TODO: ???
			Topic t = (Topic) message.getValue();
			return new PublisherDiscoveryThread(new ObjectOutputStream(connection.getOutputStream()), t);

		default:
			throw new IllegalArgumentException("You forgot to put a case for the new Message enum");
		}
	}

	/**
	 * Return the broker that's responsible for the requested topic.
	 * @param topic the topic
	 * @return the {@link ConnectionInfo} of the assigned broker
	 */
	private ConnectionInfo getAssignedBroker(Topic topic) {
		int brokerIndex = topic.hashCode() % brokerConnections.size();

		try (final Socket broker = brokerConnections.get(brokerIndex)) {
			return new ConnectionInfo(broker.getInetAddress(), broker.getPort());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// ============== UNUSED =======================

	/**
	 * Sends a message to all brokers.
	 *
	 * @param m the message
	 */
	@SuppressWarnings("unused")
	private void multicastMessage(Message m) {
		for(Socket connection : brokerConnections) {
			try(ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream())) {
				out.writeObject(m);
			} catch (IOException ioe) {
				System.err.println("Message transmission failed: " + ioe.toString());
			}
		}
	}


	/**
	 * Update internal data structures once a new connection has been established with
	 * another broker.
	 *
	 * @param newBroker the information of the connected broker
	 */
	@SuppressWarnings("unused")
	private void newBrokerConnected(Socket newBroker) {
		if(brokerConnections.contains(newBroker))
			return;

		brokerConnections.add(newBroker);
		brokerConnections.sort((s1, s2) -> {
			int ipc = s1.getInetAddress().getHostName()
			        .compareTo(s2.getInetAddress().getHostName());
			return ipc != 0 ? ipc : s2.getPort() - s1.getPort();
		});
	}

	/**
	 * Get the assigned topicsByName for this broker.
	 *
	 * @return the topicsByName
	 */
	@SuppressWarnings("unused")
	private Set<Topic> getAssignedTopics() {
		return this.postsPerTopic.keySet();
	}

	// ========== THREADS ==========

	 private class ClientRequestHandler extends Thread {

        private Socket socket;

        public ClientRequestHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

			try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
				Message m      = (Message) ois.readObject();
				Thread  thread = Broker.this.threadFactory(m, socket);
				thread.start();

				Broker.this.publisherInfo.add(new ConnectionInfo(socket.getInetAddress(), socket.getPort()));
			} catch (IOException ioe) {
				// do nothing
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
        }
    }

	/**
	 * A Thread for discovering the actual broker for a topic.
	 *
	 * @author Alex Mandelias
	 */
	private class PublisherDiscoveryThread extends Thread {

		private final ObjectOutputStream stream;
		private final Topic              topic;

		/**
		 * Constructs the Thread that, when run, will write the address of the broker
		 * that has the requested topic in the given output stream.
		 *
		 * @param stream  the output stream to which to write the data
		 * @param topic the topic
		 */
		public PublisherDiscoveryThread(ObjectOutputStream stream, Topic topic) {
			this.stream = stream;
			this.topic = topic;
		}

		@Override
		public void run() {

			try {
				ConnectionInfo brokerInfo = Broker.this.getAssignedBroker(topic);
				stream.writeObject(brokerInfo);
			} catch (IOException e) {
				// do nothing
			}	// how does the client know no reply is coming?
		}
	}
}
