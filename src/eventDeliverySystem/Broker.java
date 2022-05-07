package eventDeliverySystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import eventDeliverySystem.PushThread.Protocol;

/**
 * TODO: fix.
 * <p>
 * A remote component that forms the backbone of the EventDeliverySystem.
 * Brokers act as part of a distributed server that services Publishers and
 * Consumers.
 */
class Broker implements Runnable {

	private static final PortManager portManager     = new PortManager();
	private static final int         MAX_CONNECTIONS = 64;
	private static int clientRequestCounter = 0;

	private ServerSocket clientRequestSocket;
	private ServerSocket brokerRequestSocket;

	private final Set<ConnectionInfo>      publisherConnectionInfo;
	private final Map<String, Set<ObjectOutputStream>> consumerOOSPerTopic;

	private final Map<String, BrokerTopic> topicsByName;

	private final List<Socket> brokerConnections;

	/** Create a new Broker */
	public Broker() {
		// TODO: establish connection with broker
		this.publisherConnectionInfo = Collections.synchronizedSet(new HashSet<>());
		this.consumerOOSPerTopic = Collections.synchronizedMap(new HashMap<>());
		this.brokerConnections = Collections.synchronizedList(new LinkedList<>());
		this.topicsByName = Collections.synchronizedMap(new HashMap<>());
	}

	public static void main(String[] args) {
		LG.sout("Broker#main()");
		LG.in();
		LG.args(args);
		Broker broker = new Broker();
		Thread thread = new Thread(broker, "Broker-" + args[0]);
		thread.start();
		LG.out();
		LG.sout("/Broker#main()");
	}

	@Override
	public void run() {
		try {
			clientRequestSocket = new ServerSocket(portManager.getNewAvailablePort(), MAX_CONNECTIONS);
			brokerRequestSocket = new ServerSocket(portManager.getNewAvailablePort(), MAX_CONNECTIONS);
			LG.sout("Broker connected at:");
			LG.socket("Client", clientRequestSocket);
			LG.socket("Broker", brokerRequestSocket);

			// Start handling client requests
			Runnable clientRequestThread = () -> {
				LG.sout("Start: clientRequestThread");
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
					LG.sout("Start: BrokerRequestThread");
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

			new Thread(clientRequestThread, "ClientRequestThread").start();
			new Thread(brokerRequestThread, "BrokerRequestThread").start();

		} catch (IOException e) {
			e.printStackTrace();
		}

		LG.sout("Broker#run end");
	}


	/**
	 * Return the broker that's responsible for the requested topic.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @return the {@link ConnectionInfo} of the assigned broker
	 */
	private ConnectionInfo getAssignedBroker(String topicName) {
		int brokerIndex = AbstractTopic.hashForTopic(topicName) % (brokerConnections.size() + 1);

		// last index (out of range normally) => this broker is responsible for the topic
		// this rule should work because the default broker is the only broker that processes
		// such requests.
		if(brokerIndex == brokerConnections.size()) {
			return new ConnectionInfo(clientRequestSocket.getInetAddress(), clientRequestSocket.getLocalPort());
		}

		// else send the broker from the other connections
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
		for (Socket connection : brokerConnections) {
			try (ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream())) {
				out.writeObject(m);
			} catch (IOException ioe) {
				System.err.println("Message transmission failed: " + ioe.toString());
			}
		}
	}

	/**
	 * Update internal data structures once a new connection has been established
	 * with another broker.
	 *
	 * @param newBroker the information of the connected broker
	 */
	@SuppressWarnings("unused")
	private void newBrokerConnected(Socket newBroker) {
		if (brokerConnections.contains(newBroker))
			return;

		brokerConnections.add(newBroker);
		brokerConnections.sort((s1, s2) -> {
			int ipc = s1.getInetAddress().getHostName()
			        .compareTo(s2.getInetAddress().getHostName());
			return ipc != 0 ? ipc : s2.getPort() - s1.getPort();
		});
	}

	// ========== THREADS ==========

	private class ClientRequestHandler extends Thread {

		private Socket socket;

		public ClientRequestHandler(Socket socket) {
			super("ClientRequestHandler-" + socket.getInetAddress() + "-" + socket.getLocalPort());
			this.socket = socket;
		}

		@Override
		public void run() {

			LG.ssocket("Starting ClientRequestHandler for Socket", socket);

			try {
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				oos.flush();
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

				Message message = (Message) ois.readObject();
				LG.sout("Creating thread for message type: %s", message.getType());

				// fuck variables in switch statements
				BrokerTopic topic;
				String topicName;
				Thread thread = null;

				LG.in();
				switch (message.getType()) {
				case DATA_PACKET_SEND:
					topicName = (String) message.getValue();
					LG.sout("Receiving packets for Topic '%s'", topicName);
					LG.in();
					topic = getTopic(topicName);
					// thread = new PullThread(ois, topic);

					// TODO: fix
					long oldIdOfLast = topic.getLastPostId();
					thread = new PullThread(ois, topic);
					thread.run();

					List<Post> newPosts = topic.getPostsSince(oldIdOfLast);
					LG.sout("newPosts.size()=%d", newPosts.size());
					for (ObjectOutputStream oos1 : consumerOOSPerTopic.get(topicName)) {
						Thread pushThread = new PushThread(oos1, newPosts, Protocol.WITHOUT_COUNT);
						pushThread.start();
					}
					thread = null;
					LG.out();
					break;

				case INITIALISE_CONSUMER:
					Topic.TopicToken topicToken = (Topic.TopicToken) message.getValue();
					topicName = topicToken.getName();
					LG.sout("Registering consumer for Topic '%s'", topicName);
					LG.in();
					if (!topicsByName.containsKey(topicName))
						addTopic(topicName);

					consumerOOSPerTopic.get(topicName).add(oos);

					// send existing topics that the consumer does not have
					long idOfLast = topicToken.getLastId();
					LG.sout("idOfLast=%d", idOfLast);
					List<PostInfo> piList = new LinkedList<>();
					Map<Long, Packet[]> packetMap = new HashMap<>();
					getTopic(topicName).getAllPosts(piList, packetMap);

					LG.sout("piList=%s", piList);
					LG.sout("packetMap=%s", packetMap);
					thread = new PushThread(oos, piList, packetMap, Protocol.KEEP_ALIVE);
					LG.out();
					break;

				case PUBLISHER_DISCOVERY_REQUEST:
					addPublisherCI(socket);
					topicName = (String) message.getValue();
					thread = new PublisherDiscoveryThread(oos, topicName);
					break;

				case CREATE_TOPIC:
					topicName = (String) message.getValue();
					LG.sout("Creating Topic '%s'", topicName);

					boolean topicExists = topicsByName.containsKey(topicName);
					LG.sout("Exists '%s'", topicExists);
					if (!topicExists)
						addTopic(topicName);

					oos.writeBoolean(!topicExists);
					oos.flush();
					break;

				case BROKER_CONNECTION:
					// we don't add the socket directly because it might be from the default
					// broker who is notifying us of another connection
					ConnectionInfo newBroker = (ConnectionInfo) message.getValue();
					brokerConnections.add(new Socket(newBroker.getAddress(), newBroker.getPort()));
					//TODO: if leader send the connection to all other brokers
					break;

				default:
					throw new IllegalArgumentException(
					        "You forgot to put a case for the new Message enum");
				}
				LG.out();

				// not all MessageTypes require a thread
				if (thread != null)
					thread.start();

			} catch (IOException ioe) {
				// do nothing
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * A Thread for discovering the actual broker for a Topic.
	 *
	 * @author Alex Mandelias
	 */
	private class PublisherDiscoveryThread extends Thread {

		private final ObjectOutputStream oos;
		private final String             topicName;

		/**
		 * Constructs the Thread that, when run, will write the address of the broker
		 * that has the requested topic in the given output stream.
		 *
		 * @param stream    the output stream to which to write the data
		 * @param topicName the name of the Topic
		 */
		public PublisherDiscoveryThread(ObjectOutputStream stream, String topicName) {
			super("PublisherDiscoveryThread-" + topicName);
			oos = stream;
			this.topicName = topicName;
		}

		@Override
		public void run() {

			LG.sout("PublichserDiscoveryThread#run()");
			LG.in();

			try /* (oos) */ {
				LG.sout("topicName=%s", topicName);
				ConnectionInfo brokerInfo = Broker.this.getAssignedBroker(topicName);
				LG.sout("brokerInfo=%s", brokerInfo);
				oos.writeObject(brokerInfo);
			} catch (IOException e) {
				// do nothing
			}
			LG.out();
		}
	}

	// ==================== PRIVATE METHODS ====================

	/**
	 * Adds a new topic to the Broker's <String, Topic> map.
	 *
	 * @param topicName the name of the Topic to be added
	 */
	private void addTopic(String topicName) {
		topicsByName.put(topicName, new BrokerTopic(topicName));
		consumerOOSPerTopic.put(topicName, new HashSet<>());
	}

	private void addPublisherCI(Socket socket) {
		publisherConnectionInfo.add(new ConnectionInfo(socket.getInetAddress(), socket.getPort()));
	}

	private BrokerTopic getTopic(String topicName) {
		BrokerTopic topic = topicsByName.get(topicName);
		if (topic == null)
			throw new NoSuchElementException("There is no Topic with name " + topicName);
		return topic;
	}
}
