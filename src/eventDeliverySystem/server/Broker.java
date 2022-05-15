package eventDeliverySystem.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import eventDeliverySystem.datastructures.AbstractTopic;
import eventDeliverySystem.datastructures.ConnectionInfo;
import eventDeliverySystem.datastructures.Message;
import eventDeliverySystem.datastructures.Packet;
import eventDeliverySystem.datastructures.PostInfo;
import eventDeliverySystem.datastructures.Topic;
import eventDeliverySystem.thread.PullThread;
import eventDeliverySystem.thread.PushThread;
import eventDeliverySystem.thread.PushThread.Protocol;
import eventDeliverySystem.util.LG;
import eventDeliverySystem.util.PortManager;

/**
 * A remote component that forms the backbone of the EventDeliverySystem.
 * Brokers act as part of a distributed server that services Publishers and
 * Consumers.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
public class Broker implements Runnable, AutoCloseable {

	private static final int MAX_CONNECTIONS = 64;

	private final Map<String, Set<ObjectOutputStream>> consumerOOSPerTopic;
	private final Map<String, BrokerTopic>             topicsByName;
	private final List<Socket>                         brokerConnections;
	private final List<ConnectionInfo>                 brokerCI;

	private final ServerSocket clientRequestSocket;
	private final ServerSocket brokerRequestSocket;

	/**
	 * Create a new leader broker. This is necessarily the first step to initialize
	 * the server network.
	 */
	public Broker() {
		consumerOOSPerTopic = new HashMap<>();
		brokerConnections = new LinkedList<>();
		brokerCI = new LinkedList<>();
		topicsByName = new HashMap<>();

		try {
			clientRequestSocket = new ServerSocket(PortManager.getNewAvailablePort(),
			        Broker.MAX_CONNECTIONS);
			brokerRequestSocket = new ServerSocket(PortManager.getNewAvailablePort(),
			        Broker.MAX_CONNECTIONS);
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not opne server socket :", e);
		}

		LG.sout("Broker connected at:");
		LG.socket("Client", clientRequestSocket);
		LG.socket("Broker", brokerRequestSocket);
	}

	/**
	 * Begins listening for and new requests by clients and connection requests from
	 * other brokers.
	 */
	@Override
	public void run() {

		// Start handling client requests
		final Runnable clientRequestThread = () -> {
			LG.sout("Start: clientRequestThread");
			while (true)
				try {
					@SuppressWarnings("resource")
					final Socket socket = clientRequestSocket.accept();
					new ClientRequestHandler(socket).start();

				} catch (final IOException e) {
					e.printStackTrace();
					System.exit(-1); // serious error when waiting, close broker
				}
		};


		final Runnable brokerRequestThread = new Runnable() {

			@SuppressWarnings("resource")
			@Override
			public void run() {
				LG.sout("Start: BrokerRequestThread");
				while (true)
					try {
						final Socket socket = brokerRequestSocket.accept();
						synchronized (brokerConnections) {
							brokerConnections.add(socket);
						}

						final ObjectInputStream ois = new ObjectInputStream(
						        socket.getInputStream());
						ConnectionInfo          brokerCIForClient;
						try {
							brokerCIForClient = (ConnectionInfo) ois.readObject();
						} catch (final ClassNotFoundException e) {
							e.printStackTrace();
							return;
						}

						synchronized (brokerCI) {
							brokerCI.add(brokerCIForClient);
						}

					} catch (final IOException e) {
						e.printStackTrace();
						System.exit(-1); // serious error when waiting, close broker
					}
			}

		}; //brokerRequestThread

		new Thread(clientRequestThread, "ClientRequestThread").start();
		new Thread(brokerRequestThread, "BrokerRequestThread").start();

		LG.sout("Broker#run end");
	}


	/**
	 * Create a non-leader broker and connect it to the server network.
	 *
	 * @param leaderIP   the IP of the leader broker
	 * @param leaderPort the port of the leader broker
	 *
	 * @throws UncheckedIOException if the connection to the leader broker fails
	 */
	@SuppressWarnings("resource")
	public Broker(String leaderIP, int leaderPort) {
		this();
		try {
			final Socket             leaderConnection = new Socket(leaderIP, leaderPort);
			final ObjectOutputStream oos              = new ObjectOutputStream(
			        leaderConnection.getOutputStream());
			oos.flush();
			oos.writeObject(new ConnectionInfo(clientRequestSocket));
			oos.flush();
			brokerConnections.add(leaderConnection);
		} catch (final IOException ioe) {
			throw new UncheckedIOException("Couldn't connect to leader broker ", ioe);
		}

	}

	/**
	 * Closes all active connections to the broker.
	 */
	@Override
	synchronized public void close() {
		try {
			for (final Set<ObjectOutputStream> consumers : consumerOOSPerTopic.values())
				for (final ObjectOutputStream consumer : consumers)
					consumer.close();

			for (final Socket broker : brokerConnections)
				broker.close();

		} catch (final IOException ioe) {
			ioe.printStackTrace();
		}
	}


	// ==================== PRIVATE METHODS ====================

	/**
	 * Adds a new topic to the Broker's <String, Topic> map.
	 *
	 * @param topicName the name of the Topic to be added
	 */
	synchronized private void addTopic(String topicName) {
		topicsByName.put(topicName, new BrokerTopic(topicName));
		consumerOOSPerTopic.put(topicName, new HashSet<>());
	}

	private BrokerTopic getTopic(String topicName) {
		final BrokerTopic topic;
		synchronized (topicsByName) {
			topic = topicsByName.get(topicName);
		}
		if (topic == null)
			throw new NoSuchElementException("There is no Topic with name " + topicName);
		return topic;
	}


	/**
	 * Return the broker that's responsible for the requested Topic.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @return the {@link ConnectionInfo} for the assigned broker
	 */
	private ConnectionInfo getAssignedBroker(String topicName) {
		final int brokerIndex;
		synchronized (brokerConnections) {
			int hash = AbstractTopic.hashForTopic(topicName);
			brokerIndex = Math.abs(hash % (brokerConnections.size() + 1));

			// last index (out of range normally) => this broker is responsible for the topic
			// this rule should work because the default broker is the only broker that processes
			// such requests.
			if (brokerIndex == brokerConnections.size())
				return new ConnectionInfo(clientRequestSocket);
		}

		// else send the broker from the other connections
		synchronized (brokerCI) {
			return brokerCI.get(brokerIndex);
		}
	}


	// ========== THREADS ==========

	/**
	 * A thread which continuously reads new client requests and assigns worker
	 * threads to fulfill them when necessary.
	 *
	 * @author Alex Mandelias
	 * @author Dimitris Tsirmpas
	 */
	private class ClientRequestHandler extends Thread {

		private final Socket socket;

		public ClientRequestHandler(Socket socket) {
			super("ClientRequestHandler-" + socket.getInetAddress() + "-" + socket.getLocalPort());
			this.socket = socket;
		}

		@Override
		public void run() {

			LG.ssocket("Starting ClientRequestHandler for Socket", socket);

			try {
				final ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				oos.flush();
				final ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

				final Message message = (Message) ois.readObject();
				LG.sout("Creating thread for message type: %s", message.getType());

				// fuck variables in switch statements
				BrokerTopic topic;
				String      topicName;

				LG.in();
				switch (message.getType()) {
				case DATA_PACKET_SEND:
					topicName = (String) message.getValue();
					LG.sout("Receiving packets for Topic '%s'", topicName);
					LG.in();
					topic = getTopic(topicName);
					new PullThread(ois, topic).start();

					LG.out();
					break;

				case INITIALISE_CONSUMER:
					final Topic.TopicToken topicToken = (Topic.TopicToken) message.getValue();
					topicName = topicToken.getName();
					LG.sout("Registering consumer for Topic '%s'", topicName);
					LG.in();

					synchronized (topicsByName) {
						if (!topicsByName.containsKey(topicName))
							addTopic(topicName);
					}


					synchronized (consumerOOSPerTopic) {
						consumerOOSPerTopic.get(topicName).add(oos);
					}

					synchronized (topicsByName) {
						topic = topicsByName.get(topicName);
					}
					new BrokerPushThread(topic, oos).start();

					// send existing topics that the consumer does not have
					final long idOfLast = topicToken.getLastId();
					LG.sout("idOfLast=%d", idOfLast);
					final List<PostInfo> piList = new LinkedList<>();
					final Map<Long, Packet[]> packetMap = new HashMap<>();
					getTopic(topicName).getPostsSince(idOfLast, piList, packetMap);

					LG.sout("piList=%s", piList);
					LG.sout("packetMap=%s", packetMap);
					new PushThread(oos, piList, packetMap, Protocol.KEEP_ALIVE).start();
					LG.out();
					break;

				case BROKER_DISCOVERY:
					topicName = (String) message.getValue();
					new BrokerDiscoveryThread(oos, topicName).start();
					break;

				case CREATE_TOPIC:
					topicName = (String) message.getValue();
					LG.sout("Creating Topic '%s'", topicName);

					final boolean topicExists;
					synchronized (topicsByName) {
						topicExists = topicsByName.containsKey(topicName);
					}
					LG.sout("Exists '%s'", topicExists);
					if (!topicExists)
						addTopic(topicName);

					oos.writeBoolean(!topicExists);
					oos.flush();
					break;

				default:
					throw new IllegalArgumentException(
					        "You forgot to put a case for the new Message enum");
				}
				LG.out();

			} catch (final IOException ioe) {
				// do nothing
			} catch (final ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * A Thread for discovering the actual Broker for a Topic.
	 *
	 * @author Alex Mandelias
	 */
	private class BrokerDiscoveryThread extends Thread {

		private final ObjectOutputStream oos;
		private final String             topicName;

		/**
		 * Constructs the Thread that, when run, will write the ConnectionInfo of the
		 * Broker responsible for the requested Topic to the given output stream.
		 *
		 * @param stream    the output stream to which to write the ConnectionInfo
		 * @param topicName the name of the Topic
		 */
		public BrokerDiscoveryThread(ObjectOutputStream stream, String topicName) {
			super("BrokerDiscoveryThread-" + topicName);
			oos = stream;
			this.topicName = topicName;
		}

		@Override
		public void run() {

			LG.sout("BrokerDiscoveryThread#run()");
			LG.in();

			LG.sout("topicName=%s", topicName);
			final ConnectionInfo brokerInfo = getAssignedBroker(topicName);
			LG.sout("brokerInfo=%s", brokerInfo);

			try {
				oos.writeObject(brokerInfo);
			} catch (final IOException e) {
				// do nothing
			}
			LG.out();
		}
	}
}
