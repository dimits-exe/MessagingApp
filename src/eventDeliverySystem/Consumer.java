
package eventDeliverySystem;

import static eventDeliverySystem.Message.MessageType.DATA_PACKET_RECEIVE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * TODO: implement.
 * <p>
 * A process that initializes connections to brokers to receive data.
 *
 * @author Alex Mandelias
 */
class Consumer implements Runnable, AutoCloseable {

	private final CIManager topicCIManager;

	// TODO: use this shit or change it to something else idk
	private final Map<String, Topic> topics;

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
	 * TODO: rewrite this shit
	 * <p>
	 * Pulls a number of Posts from a Topic.
	 *
	 * @param topicName the name of the Topic from which to pull
	 *
	 * @return a list of the Posts read
	 */
	public List<Post> pull(String topicName) {

		boolean success;
		PullThread pullThread = null;

		do {
			ConnectionInfo actualBrokerCI = topicCIManager.getConnectionInfoForTopic(topicName);

			try (Socket socket = new Socket(actualBrokerCI.getAddress(), actualBrokerCI.getPort())) {

				try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

					long idOfLast = topics.get(topicName).getIdOfLastTopicPleaseOkayThanks()
					        .getPostInfo().getId();

					oos.writeObject(new Message(DATA_PACKET_RECEIVE,
					        new Topic.TopicPointer(topicName, idOfLast)));

					pullThread = new PullThread(ois, topicName);
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

		List<Post> postsRead = pullThread.posts; // can't be null

		postsPerTopic.get(topicName).addAll(postsRead);

		return postsRead;
	}

	@Override
	public void close() throws IOException {
		topicCIManager.close();
	}

	/**
	 * TODO: probably nothing, this seems fine
	 * <p>
	 * A Thread for sending some data to an output stream.
	 *
	 * @author Alex Mandelias
	 */
	private static class PullThread extends Thread {

		private final ObjectInputStream stream;
		private boolean                 success, start, end;

		private final List<Post> posts;

		/**
		 * Constructs the Thread that, when run, will read data from the stream.
		 *
		 * @param stream the input stream from which to read the data
		 */
		public PullThread(ObjectInputStream stream) {
			this.stream = stream;
			success = start = end = false;
			posts = new LinkedList<>();
		}

		@Override
		public void run() {
			start = true;

			final List<Packet> postFragments = new LinkedList<>();

			try {
				Integer postCount;
				try {
					postCount = (Integer) stream.readObject();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					return;
				}

				for (int i = 0; i < postCount; i++) {
					PostInfo postInfo;
					try {
						postInfo = (PostInfo) stream.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}

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

					final Packet[] temp    = new Packet[postFragments.size()];
					final Packet[] packets = postFragments.toArray(temp);
					final Post     post    = Post.fromPackets(packets, postInfo);
					posts.add(post);
				}

				success = true;

			} catch (IOException e) {
				System.err.printf("IOException while receiving packets from actual broker%n");
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
