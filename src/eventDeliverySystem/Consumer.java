package eventDeliverySystem;

import static eventDeliverySystem.Message.MessageType.INITIALISE_CONSUMER;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A process that holds the posts of a certain user and updates them by
 * connecting to a remote server.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
class Consumer extends ClientNode {

	private final Map<String, Topic> topicsByName;
	private final Map<String, Socket> brokersForTopic;

	// TODO: inform user when a new Post for a Topic arrives

	/**
	 * Constructs a Consumer that will connect to a specific default broker.
	 *
	 * @param defaultServerIP   the IP of the default broker, interpreted as
	 *                          {@link InetAddress#getByName(String)}.
	 * @param defaultServerPort the port of the default broker
	 * @param topics            the Topics for which this Consumer listens
	 *
	 * @throws UnknownHostException if no IP address for the host could be found, or
	 *                              if a scope_id was specified for a global IPv6
	 *                              address while resolving the defaultServerIP.
	 * @throws IOException          if an I/O error occurs when opening the
	 *                              Publisher's Server Socket.
	 */
	public Consumer(String defaultServerIP, int defaultServerPort, Set<Topic> topics)
	        throws IOException {
		this(InetAddress.getByName(defaultServerIP), defaultServerPort, topics);
	}

	/**
	 * Constructs a Consumer that will connect to a specific default broker.
	 *
	 * @param defaultServerIP   the IP of the default broker, interpreted as
	 *                          {@link InetAddress#getByAddress(byte[])}.
	 * @param defaultServerPort the port of the default broker
	 * @param topics            the Topics for which this Consumer listens
	 *
	 * @throws UnknownHostException if IP address is of illegal length
	 * @throws IOException          if an I/O error occurs when opening the
	 *                              Publisher's Server Socket.
	 */
	public Consumer(byte[] defaultServerIP, int defaultServerPort, Set<Topic> topics)
	        throws IOException {
		this(InetAddress.getByAddress(defaultServerIP), defaultServerPort, topics);
	}

	/**
	 * Constructs a Consumer that will connect to a specific default broker.
	 *
	 * @param ip     the InetAddress of the default broker
	 * @param port   the port of the default broker
	 * @param topics the Topics for which this Consumer listens
	 *
	 * @throws IOException if an I/O error occurs while initialising the Client Node
	 */
	protected Consumer(InetAddress ip, int port, Set<Topic> topics) throws IOException {
		super(ip, port);

		brokersForTopic = new HashMap<>();
		topicsByName = new HashMap<>();

		for (Topic topic : topics)
			topicsByName.put(topic.getName(), topic);

		connectionSetup();
	}

	@Override
	protected void closeImpl() throws IOException {
		for (Socket socket : brokersForTopic.values())
			socket.close();
	}

	/**
	 * Returns all Posts from a Topic.
	 *
	 * @param topicName the name of the Topic
	 *
	 * @return a List with all the Posts of the Topic
	 */
	public List<Post> pull(String topicName) {
		return topicsByName.get(topicName).getAllPosts();
	}

	@SuppressWarnings("resource")
	private void connectionSetup() {

		List<String> topicNames = new ArrayList<>(topicsByName.keySet());

		for (int i = 0; i < topicNames.size(); i++) {
			String         topicName = topicNames.get(i);
			ConnectionInfo ci = topicCIManager.getConnectionInfoForTopic(topicName);

			try (Socket socket = new Socket(ci.getAddress(), ci.getPort())) {
				// save open socket, don't send anything yet
				brokersForTopic.put(topicName, socket); // closes at close1()
			} catch (IOException e) {
				// invalidate and ask again for topicName;
				topicCIManager.invalidate(topicName);
				i--;
			}
		}

		// send INITIALISE_CONSUMER message to every socket
		for (Entry<String, Socket> e : brokersForTopic.entrySet()) {
			Socket socket = e.getValue(); // closes at close1()

			try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
				ObjectInputStream  ois = new ObjectInputStream(socket.getInputStream());

				Topic topic = topicsByName.get(e.getKey());
				oos.writeObject(new Message(INITIALISE_CONSUMER, topic.getToken()));

				new PullThread(ois, topic).start();

			} catch (IOException e1) {
				throw new UncheckedIOException(e1); // closes at close1()
			}
		}
	}
}
