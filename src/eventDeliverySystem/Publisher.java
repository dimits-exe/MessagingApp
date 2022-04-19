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

		final Packet[] packets = Packet.fromPost(post);
		final Topic    topic   = post.getPostInfo().getTopic();

		boolean success;

		do {
			ConnectionInfo actualBrokerCI = topicCIManager.getConnectionInfoForTopic(topic);

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
				topicCIManager.invalidate(topic);
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

	/**
	 * Wrapper for a cache that communicates with the default broker to store,
	 * update and invalidate the ConnectionInfoes for Topics. A ServerSocket is also
	 * opened for receiving messages from brokers, which is closed by calling the
	 * {@code close()} method.
	 *
	 * @author Alex Mandelias
	 */
	private static class CIManager implements AutoCloseable {

		private static final PortManager PORT_MANAGER = new PortManager();

		private final Map<Topic, ConnectionInfo> map;
		private ConnectionInfo                   defaultBrokerCI;
		private final ServerThread               serverThread;

		private class ServerThread extends Thread implements AutoCloseable {

			private final ServerSocket serverSocket;

			/**
			 * Constructs a new ServerThread by opening this CIManager's ServerSocket.
			 *
			 * @throws IOException if an I/O error occurs when opening this CIManager's
			 *                     Server Socket.
			 */
			public ServerThread() throws IOException {
				serverSocket = new ServerSocket(PORT_MANAGER.getNewAvailablePort());
			}

			@Override
			public void run() {
				while (true) {
					boolean ipForNewDefaultBrokerException;

					do {
						ipForNewDefaultBrokerException = false;

						try (Socket socket1 = serverSocket.accept()) {
							try (ObjectInputStream ois1 = new ObjectInputStream(
							        socket1.getInputStream())) {
								try {
									defaultBrokerCI = (ConnectionInfo) ois1.readObject();
								} catch (ClassNotFoundException e1) {
									e1.printStackTrace();
									return;
								} finally {
									ois1.close();
								}
							} catch (IOException e1) {
								// io exception while getting streams, keep trying
								ipForNewDefaultBrokerException = true;
								System.err.printf(
								        "IOException while getting new ConnectionInfo for default broker%n");
							}
						} catch (IOException e2) {
							// server socket was closed (presumably by calling close()), stop this thread's execution
							System.err.printf(
							        "IOException while waiting for connection for new default broker%n");

							return;
						}
					} while (ipForNewDefaultBrokerException);

					CIManager.this.notify(); // notify
				}
			}

			@Override
			public void close() throws IOException {
				serverSocket.close();
			}
		}

		/**
		 * Constructs the CIManager given the ConnectionInfo to the default broker. The
		 * CIManager's server socket is also opened and begins listening for connections
		 * to update its connection information for the default broker.
		 *
		 * @param defaultBrokerConnectionInfo the IP of the default broker to connect to
		 *
		 * @throws IOException if an I/O error occurs when opening this IP Manager's
		 *                     Server Socket.
		 */
		public CIManager(ConnectionInfo defaultBrokerConnectionInfo) throws IOException {
			map = new HashMap<>();
			defaultBrokerCI = defaultBrokerConnectionInfo;
			serverThread = new ServerThread();
		}

		/**
		 * Communicates with the default broker to fetch the ConnectionInfo associated with
		 * a Topic, which is then cached. Future requests for it will use the
		 * ConnectionInfo found in the cache.
		 * <p>
		 * To invalidate the cache and request that the default broker provide a new
		 * ConnectionInfo, the {@link #invalidate(Topic)} method may be used.
		 *
		 * @param topic the Topic for which to get the ConnectionInfo
		 *
		 * @return the ConnectionInfo for that Topic
		 */
		public ConnectionInfo getConnectionInfoForTopic(Topic topic) {
			ConnectionInfo address = map.get(topic);

			if (address != null)
				return address;

			updateCIForTopic(topic);
			return map.get(topic);
		}

		/**
		 * Invalidates the ConnectionInfo associated with a Topic. The next time the
		 * ConnectionInfo for said Topic is requested, the default broker will be asked
		 * will be requested to provide it.
		 *
		 * @param topic the Topic for which to invalidate the ConnectionInfo
		 */
		public void invalidate(Topic topic) {
			map.remove(topic);
		}

		@Override
		public void close() throws IOException {
			serverThread.close();
		}

		private void updateCIForTopic(Topic topic) {
			boolean ipForTopicBrokerException;
			do {
				ipForTopicBrokerException = false;

				try (Socket socket = getSocketToDefaultBroker();
				        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

					oos.writeObject(new Message(DISCOVER, topic));

					ConnectionInfo actualBrokerCIForTopic;
					try {
						actualBrokerCIForTopic = (ConnectionInfo) ois.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}

					map.put(topic, actualBrokerCIForTopic);

				} catch (IOException e) {
					ipForTopicBrokerException = true;

					System.err
					        .printf("IOException while getting ConnectionInfo for Topic from default broker%n");

					try {
						// wait until notified by server thread that the default broker has been changed
						wait();
					} catch (InterruptedException e1) {
						System.err
						        .printf("Interrupted after IOException while getting ConnectionInfo for Topic from default broker%n");
					}
				}
			} while (ipForTopicBrokerException);
		}

		private Socket getSocketToDefaultBroker() throws IOException {
			return new Socket(defaultBrokerCI.getAddress(), defaultBrokerCI.getPort());
		}
	}
}
