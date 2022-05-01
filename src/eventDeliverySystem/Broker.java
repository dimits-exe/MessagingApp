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

import eventDeliverySystem.Topic.TopicToken;

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
	private final Map<String, Set<Socket>> consumerConnectionsPerTopic;

	private final Map<String, Topic> topicsByName;

	private final List<Socket> brokerConnections;

	/** Create a new Broker */
	public Broker() {
		// TODO: establish connection with broker
		this.publisherConnectionInfo = Collections.synchronizedSet(new HashSet<>());
		this.consumerConnectionsPerTopic = Collections.synchronizedMap(new HashMap<>());
		this.brokerConnections = Collections.synchronizedList(new LinkedList<>());
		this.topicsByName = Collections.synchronizedMap(new HashMap<>());
	}

	public static void main(String[] args) {
		LG.sout("Broker#main start");
		LG.args(args);
		Broker broker = new Broker();
		Thread thread = new Thread(broker, "Broker-" + args[0]);
		thread.start();
		LG.sout("Broker#main end");
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

			new Thread(clientRequestThread).start();
			new Thread(brokerRequestThread).start();

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
		int brokerIndex = getTopic(topicName).hashCode() % (brokerConnections.size() + 1);
		
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
	
	/**
	 * Adds a new topic to the Broker's <String, Topic> map.
	 * @param topic the topic to be added
	 */
	private void addTopic(Topic topic) {
		topicsByName.put(topic.getName(), topic);
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
			super("ClientRequestHandler-" + clientRequestCounter++);
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
				Topic  topic;
				String topicName;
				Thread thread = null;

				switch (message.getType()) {
				case DATA_PACKET_SEND:
					topicName = (String) message.getValue();
					topic = getTopic(topicName);
					thread = new PullThread(ois, topic);
					break;

				case INITIALISE_CONSUMER:
					TopicToken topicToken = (TopicToken) message.getValue();
					topicName = topicToken.getName();
					long idOfLast = topicToken.getLastId();

					registerConsumerForTopic(topicName, socket);

					// send existing topics that the consumer does not have
					List<Post> postsToSend = getTopic(topicName).getPostsSince(idOfLast);
					thread = new PushThread(oos, postsToSend, true); // keep consumer's thread alive
					break;

				case PUBLISHER_DISCOVERY_REQUEST:
					addPublisherCI(socket);
					topicName = (String) message.getValue();
					thread = new PublisherDiscoveryThread(oos, topicName);
					break;
				
				case CREATE_TOPIC:
					topicName = (String) message.getValue();
					
					boolean topicExists = topicsByName.containsKey(topicName);
					if (!topicExists)
						addTopic(new Topic(topicName));
					
					oos.writeBoolean(!topicExists);
					break;
				
				case BROKER_CONNECTION:
					// we don't add the socket directly because it might be from the default
					// broker who is notifying us of another connection
					ConnectionInfo newBroker = (ConnectionInfo) message.getValue();
					brokerConnections.add(new Socket(newBroker.getAddress(), newBroker.getPort()));
					//TODO: if leader send the connection to all other brokers
						
					return;
					
				default:
					throw new IllegalArgumentException(
					        "You forgot to put a case for the new Message enum");
				}

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
			oos = stream;
			this.topicName = topicName;
		}

		@Override
		public void run() {

			LG.sout("Sending CI to publisher");

			try (oos) {
				ConnectionInfo brokerInfo = Broker.this.getAssignedBroker(topicName);
				oos.writeObject(brokerInfo);
			} catch (IOException e) {
				// do nothing
			}
		}
	}

	// ==================== PRIVATE METHODS ====================

	private void addPublisherCI(Socket socket) {
		publisherConnectionInfo.add(new ConnectionInfo(socket.getInetAddress(), socket.getPort()));
	}

	private void registerConsumerForTopic(String topicName, Socket socket) {
		consumerConnectionsPerTopic.get(topicName).add(socket);
	}

	private Topic getTopic(String topicName) {
		Topic topic = topicsByName.get(topicName);
		if (topic == null)
			throw new NoSuchElementException("There is no Topic with name " + topicName);
		return topic;
	}
}
