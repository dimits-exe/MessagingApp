package eventDeliverySystem;

import static eventDeliverySystem.Message.MessageType.DATA_PACKET;
import static eventDeliverySystem.Message.MessageType.DISCOVER;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

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

					oos.writeObject(new Message(DATA_PACKET));

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

	/**
	 * A Thread for sending some data to an output stream.
	 *
	 * @author Alex Mandelias
	 */
	private static class PushThread extends Thread {

		private final Packet[]           data;
		private final ObjectOutputStream stream;
		private boolean                  success, start, end;

		/**
		 * Constructs the Thread that, when run, will write the data to the stream.
		 *
		 * @param data   the data to write
		 * @param stream the output stream to which to write the data
		 */
		public PushThread(Packet[] data, ObjectOutputStream stream) {
			this.data = data;
			this.stream = stream;
			success = start = end = false;
		}

		@Override
		public void run() {
			start = true;

			try {
				for (int i = 0; i < data.length; i++)
					stream.writeObject(data[i]);

				success = true;

			} catch (IOException e) {
				System.err.printf("IOException while sending packets to actual broker%n");
				success = false;
			}

			end = true;
		}

		/**
		 * Returns whether this Thread has executed its job successfully. This method
		 * shall be called after this Thread has executed its {@code run} method once.
		 *
		 * @return {@code true} if it has, {@code false} otherwise
		 *
		 * @throws IllegalStateException if this Thread has not completed its execution
		 *                               before this method is called
		 */
		public boolean success() throws IllegalStateException {
			if (!start)
				throw new IllegalStateException(
				        "Can't call 'success()' before starting this Thread");
			if (!end)
				throw new IllegalStateException(
				        "This Thread must finish execution before calling 'success()'");

			return success;
		}
	}
}
