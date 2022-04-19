package eventDeliverySystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

/**
 * A remote component that forms the backbone of the EventDeliverySystem.
 * Brokers act as part of a distributed server that services
 * Publishers and Consumers.
 *
 */
class Broker implements Runnable {

	private static final PortManager portManager = new PortManager();
	private static final int MAX_CONNECTIONS = 64;

	// TODO: this class
	private final BrokerManager BROKER_MANAGER;
	// TODO: figure out if this can be final
	private ConnectionInfo CI;

	// === PLACEHOLDERS ===

	private final Set<Socket> clientConnections; //replace with set<InetAddress>?
	private final Set<Socket> brokerConnections;
	private final Map<Topic, LinkedList<Post>> postsPerTopic;
	private final Map<Topic, LinkedList<Post>> postsBackup;

	private ServerSocket clientRequestSocket;
	private ServerSocket brokerRequestSocket;
	private boolean isLeader;

	// === END PLACEHOLDERS ===

	/**
	 * Create a new broker and add it to the already existing
	 * distributed server system.
	 * @param leaderIP the IP of the broker acting as the leader of the system
	 */
	public Broker(InetAddress leaderIP) {
		this();
		subscribeToNewBroker(leaderIP);
		isLeader = false;
	}

	/**
	 * Create a new broker that will act as the Leader of the
	 * distributed server.
	 */
	public Broker() {
		this.clientConnections = new HashSet<>();
		this.brokerConnections = new HashSet<>();
		this.postsPerTopic = Collections.synchronizedMap(new HashMap<>());
		this.postsBackup = new HashMap<>();
		isLeader = true;
	}

	/**
	 * A Thread for receiving some data from an input stream.
	 *
	 * @author Alex Mandelias
	 */
	private class PullThread extends Thread {

		private final ObjectInputStream stream;

		/**
		 * Constructs the Thread that, when run, will read data from the stream.
		 *
		 * @param stream the input stream from which to read the data
		 */
		public PullThread(ObjectInputStream stream) {
			this.stream = stream;
		}

		@Override
		public void run() {

			final List<Packet> postFragments = new LinkedList<>();

			try {
				Packet packet;
				do {
					try {
						packet = (Packet) stream.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}
					postFragments.add(packet);
				} while (!packet.isFinal());

				final Packet[] packets = postFragments.toArray(new Packet[postFragments.size()]);
				final Post     post    = Post.fromPackets(packets);
				postsPerTopic.get(post.getPostInfo().getTopic()).add(post);

				// TODO: multicast this post to every broker

			} catch (IOException e) {
				// do nothing
			}
		}
	}

	/**
	 * A Thread for TODO
	 *
	 * @author Alex Mandelias
	 */
	private class DiscoveryThread extends Thread {

		private final ObjectOutputStream stream;
		private final Topic              topic;

		/**
		 * Constructs the Thread that, when run, will TODO
		 *
		 * @param stream  the output stream to which to write the data
		 * @param message the Message that contains the Topic
		 */
		public DiscoveryThread(ObjectOutputStream stream, Message message) {
			this.stream = stream;
			this.topic = (Topic) message.getValue();
		}

		@Override
		public void run() {

			try {

				Broker broker = BROKER_MANAGER.getBroker(topic.getName());
				stream.writeObject(broker.CI);

				// TODO: update this' and other broker's ip table with this producer

			} catch (IOException e) {
				// do nothing
			}
		}
	}

	@Override
	public void run() {
		try {
			clientRequestSocket = new ServerSocket(portManager.getNewAvailablePort(), MAX_CONNECTIONS);
			brokerRequestSocket = new ServerSocket(portManager.getNewAvailablePort(), MAX_CONNECTIONS);

			Runnable clientRequestThread = new Runnable() {

				@Override
				public void run() {
					while(true) {
						try {
							clientConnections.add(clientRequestSocket.accept());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

			}; //clientRequestThread

			Runnable brokerRequestThread = new Runnable() {

				@Override
				public void run() {
					while(true) {
						try {
							brokerConnections.add(brokerRequestSocket.accept());
						} catch (IOException e) {
							e.printStackTrace();
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
	 * Add a connection from and to the new broker.
	 * @param newBrokerIP the IP of the new broker
	 */
	private void subscribeToNewBroker(InetAddress newBrokerIP) {
		try {
			brokerConnections.add(new Socket(newBrokerIP, portManager.getNewAvailablePort()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send a {@link Message} redirecting the client to the
	 * broker assigned to the topic.
	 *
	 * @param topic the topic
	 */
	private void deferToActualBroker(Topic topic) {
		if(!isLeader) {
			throw new IllegalStateException("Non-leader broker asked to redirect to other broker");
		}

	}

	/**
	 * Sends a message to all brokers.
	 * @param m the messsage
	 */
	private void multicastMessage(Message m) {
		try {
			for(Socket connection : brokerConnections) {
				ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
				out.writeObject(m);
			}
		} catch (IOException ioe) {
			System.err.println("Message transmission failed: " + ioe.toString());
		}
	}

	/**
	 * Sends a post to all brokers to be saved remotely.
	 * @param postPackets the packets of the post
	 */
	private void multicastPost(Packet[] postPackets) {
		try {
			for(Socket connection : brokerConnections) {
				ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());

				for(Packet p: postPackets) {
					out.writeObject(p);
				}

			}
		} catch (IOException ioe) {
			System.err.println("Backup failed: " + ioe.toString());
		}
	}


}
