package eventDeliverySystem;

import static eventDeliverySystem.Message.MessageType.DATA_PACKET_RECEIVE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * A process that holds the posts of a certain user and updates them
 * by connecting to a remote server.
 * 
 */
class Consumer implements Runnable, AutoCloseable {

	private final CIManager topicCIManager;

	// TODO: use this shit or change it to something else idk
	private final Map<String, Topic> topics;
	
	//TODO: add Profile to constructor
	//TODO: implement getting a list of sockets from broker
	//TODO: keep said sockets connected and choose between them (no CIManager ?)

	/**
	 * Constructs a Publisher that will connect to a specific default broker.
	 *
	 * @param defaultServerIP   the IP of the default broker, interpreted as
	 *                          {@link InetAddress#getByName(String)}.
	 * @param defaultServerPort the port of the default broker
	 *
	 * @throws UnknownHostException if no IP address for the host could be found, or
	 *                              if a scope_id was specified for a global IPv6
	 *                              address while resolving the defaultServerIP.
	 * @throws IOException          if an I/O error occurs when opening the
	 *                              Publisher's Server Socket.
	 */
	public Consumer(String defaultServerIP, int defaultServerPort) throws IOException {
		this(InetAddress.getByName(defaultServerIP), defaultServerPort);
	}

	/**
	 * Constructs a Publisher that will connect to a specific default broker.
	 *
	 * @param defaultServerIP   the IP of the default broker, interpreted as
	 *                          {@link InetAddress#getByAddress(byte[])}.
	 * @param defaultServerPort the port of the default broker
	 *
	 * @throws UnknownHostException if IP address is of illegal length
	 * @throws IOException          if an I/O error occurs when opening the
	 *                              Publisher's Server Socket.
	 */
	public Consumer(byte[] defaultServerIP, int defaultServerPort) throws IOException {
		this(InetAddress.getByAddress(defaultServerIP), defaultServerPort);
	}

	private Consumer(InetAddress ip, int port) throws IOException {
		topicCIManager = new CIManager(new ConnectionInfo(ip, port));
		topics = new HashMap<>();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	/**
	 * Updates the local Topic with new posts streamed by a
	 * remote Topic.
	 *
	 * @param topicName the name of the Topic from which to pull
	 */
	public void pull(String topicName) {

		boolean success;
		PullThread pullThread = null;
		Topic relevantTopic = topics.get(topicName);

		do {
			ConnectionInfo actualBrokerCI = topicCIManager.getConnectionInfoForTopic(topicName);

			try (Socket socket = new Socket(actualBrokerCI.getAddress(), actualBrokerCI.getPort())) {

				try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
					
					// get last post from the topic and send it to the broker
					long idOfLast = relevantTopic.getLastPost().getPostInfo().getId();
					oos.writeObject(new Message(DATA_PACKET_RECEIVE, idOfLast));
					
					// receive broker's answer on how many posts need to be sent
					// so the local topic is updated
					int postCount;
					try {
						postCount = (Integer) ois.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}
					
					// begin downloading the posts
					pullThread = new PullThread(ois, relevantTopic, postCount);
					pullThread.start();
					try {
						pullThread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				success = pullThread.success();

			} catch (IOException e) {

				System.err.printf("IOException while connecting to actual broker%n");

				success = false;
				topicCIManager.invalidate(topicName);
			}
		} while (!success);

	}

	@Override
	public void close() throws IOException {
		topicCIManager.close();
	}

}
