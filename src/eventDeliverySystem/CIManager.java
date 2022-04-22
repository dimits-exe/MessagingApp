package eventDeliverySystem;

import static eventDeliverySystem.Message.MessageType.DISCOVER;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for a cache that communicates with the default broker to store,
 * update and invalidate the ConnectionInfoes for Topics. A ServerSocket is also
 * opened for receiving messages from brokers, which is closed by calling the
 * {@code close()} method.
 *
 * @author Alex Mandelias
 */
class CIManager implements AutoCloseable {

	private static final PortManager PORT_MANAGER = new PortManager();

	private final Map<String, ConnectionInfo> map;
	private ConnectionInfo                    defaultBrokerCI;
	private final ServerThread                serverThread;

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
	 * Communicates with the default broker to fetch the ConnectionInfo associated
	 * with a Topic, which is then cached. Future requests for it will use the
	 * ConnectionInfo found in the cache.
	 * <p>
	 * To invalidate the cache and request that the default broker provide a new
	 * ConnectionInfo, the {@link #invalidate(String)} method may be used.
	 *
	 * @param topicName the Topic for which to get the ConnectionInfo
	 *
	 * @return the ConnectionInfo for that Topic
	 */
	public ConnectionInfo getConnectionInfoForTopic(String topicName) {
		ConnectionInfo address = map.get(topicName);

		if (address != null)
			return address;

		updateCIForTopic(topicName);
		return map.get(topicName);
	}

	/**
	 * Invalidates the ConnectionInfo associated with a Topic. The next time the
	 * ConnectionInfo for said Topic is requested, the default broker will be asked
	 * will be requested to provide it.
	 *
	 * @param topicName the Topic for which to invalidate the ConnectionInfo
	 */
	public void invalidate(String topicName) {
		map.remove(topicName);
	}

	@Override
	public void close() throws IOException {
		serverThread.close();
	}

	private void updateCIForTopic(String topicName) {
		boolean ipForTopicBrokerException;
		do {
			ipForTopicBrokerException = false;

			try (Socket socket = getSocketToDefaultBroker();
			        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

				oos.writeObject(new Message(DISCOVER, topicName));

				ConnectionInfo actualBrokerCIForTopic;
				try {
					actualBrokerCIForTopic = (ConnectionInfo) ois.readObject();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					return;
				}

				map.put(topicName, actualBrokerCIForTopic);

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