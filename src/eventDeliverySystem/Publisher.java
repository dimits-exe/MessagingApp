package eventDeliverySystem;

import static eventDeliverySystem.Message.MessageType.DATA_PACKET_SEND;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * A process that initializes connections to brokers to send data.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirbas
 */
class Publisher extends ClientNode {
	
	/**
	 * Constructs a Publisher that will connect to a specific default broker.
	 *
	 * @see {@link ClientNode#UserNode(String, int)}
	 */
	public Publisher(String defaultServerIP, int defaultServerPort) throws IOException {
		this(InetAddress.getByName(defaultServerIP), defaultServerPort);
	}

	/**
	 * Constructs a Publisher that will connect to a specific default broker.
	 *
	 * @see {@link ClientNode#UserNode(byte[], int)}
	 */
	public Publisher(byte[] defaultServerIP, int defaultServerPort) throws IOException {
		this(InetAddress.getByAddress(defaultServerIP), defaultServerPort);
	}

	protected Publisher(InetAddress ip, int port) throws IOException {
		super(ip, port);
	}

	@Override
	public void run() {
		// this probably should go to the constructor

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
