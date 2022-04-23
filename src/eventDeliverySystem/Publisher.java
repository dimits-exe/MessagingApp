package eventDeliverySystem;

import static eventDeliverySystem.Message.MessageType.DATA_PACKET_SEND;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * A process that initializes connections to brokers to send data.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirbas
 */
class Publisher implements Runnable, AutoCloseable {

	private final CIManager topicCIManager;

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
	public Publisher(String defaultServerIP, int defaultServerPort) throws IOException {
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
	public Publisher(byte[] defaultServerIP, int defaultServerPort) throws IOException {
		this(InetAddress.getByAddress(defaultServerIP), defaultServerPort);
	}

	private Publisher(InetAddress ip, int port) throws IOException {
		topicCIManager = new CIManager(new ConnectionInfo(ip, port));
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	/**
	 * Pushes some Data to a Topic as a specific File Type.
	 *
	 * @param post          the data
	 */
	public void push(Post post) {

		final Packet[] packets   = Packet.fromPost(post);
		final PostInfo postInfo  = post.getPostInfo();
		final String   topicName = postInfo.getTopicName();

		boolean success;

		do {
			ConnectionInfo actualBrokerCI = topicCIManager
			        .getConnectionInfoForTopic(topicName);

			try (Socket socket = new Socket(actualBrokerCI.getAddress(), actualBrokerCI.getPort())) {

				PushThread pushThread;
				try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

					oos.writeObject(new Message(DATA_PACKET_SEND, postInfo));

					pushThread = new PushThread(packets, oos);
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
