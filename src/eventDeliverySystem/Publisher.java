package eventDeliverySystem;

import static eventDeliverySystem.Message.MessageType.DATA_PACKET_SEND;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

/**
 * A process that initialises connections to brokers to send data.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirbas
 */
class Publisher extends ClientNode {

	/**
	 * Constructs a Publisher.
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
	public Publisher(String defaultServerIP, int defaultServerPort) throws IOException {
		this(InetAddress.getByName(defaultServerIP), defaultServerPort);
	}

	/**
	 * Constructs a Publisher.
	 *
	 * @param defaultServerIP   the IP of the default broker, interpreted as
	 *                          {@link InetAddress#getByAddress(byte[])}.
	 * @param defaultServerPort the port of the default broker
	 *
	 * @throws UnknownHostException if IP address is of illegal length
	 * @throws IOException          if an I/O error occurs when opening the
	 *                              Publisher's Server Socket.
	 */
	public Publisher(byte[] defaultServerIP, int defaultServerPort) throws IOException {
		this(InetAddress.getByAddress(defaultServerIP), defaultServerPort);
	}

	/**
	 * Constructs a Publisher.
	 *
	 * @param ip   the InetAddress of the default broker
	 * @param port the port of the default broker
	 *
	 * @throws IOException if an I/O error occurs while initialising the Client Node
	 */
	protected Publisher(InetAddress ip, int port) throws IOException {
		super(ip, port);
	}

	/**
	 * Pushes some Data to a Topic as a specific File Type.
	 *
	 * @param post          the data
	 */
	public void push(Post post) {

		LG.sout("Pushing: %s", post);

		final String topicName = post.getPostInfo().getTopicName();

		boolean success;

		do {
			ConnectionInfo actualBrokerCI = topicCIManager
			        .getConnectionInfoForTopic(topicName);

			LG.sout("Actual Broker CI: %s", actualBrokerCI);

			try (Socket socket = new Socket(actualBrokerCI.getAddress(), actualBrokerCI.getPort())) {

				PushThread pushThread;
				try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

					oos.writeObject(new Message(DATA_PACKET_SEND, topicName));

					pushThread = new PushThread(oos, List.of(post), false);
					pushThread.start();
					try {
						pushThread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				success = pushThread.success();

			} catch (IOException e) {

				System.err.printf("IOException while connecting to actual broker%n");
				e.printStackTrace();

				success = false;
				topicCIManager.invalidate(topicName);
			}
		} while (!success);
	}
}
