package eventDeliverySystem;

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

	/** The port used by Brokers to communicate with Publishers. */
	static final int PUBLIC_SERVER_PORT = 49674;

	private final IPManager topicIPManager;

	/**
	 * Constructs a Publisher that will connect to a specific default broker.
	 *
	 * @param defaultServerIP the IP of the default broker, interpreted as
	 *                        {@link InetAddress#getByName(String)}.
	 *
	 * @throws UnknownHostException if no IP address for the host could be found, or
	 *                              if a scope_id was specified for a global IPv6
	 *                              address while resolving the defaultServerIP.
	 * @throws IOException          if an I/O error occurs when opening the
	 *                              Publisher's Server Socket.
	 */
	public Publisher(String defaultServerIP) throws IOException {
		topicIPManager = new IPManager(InetAddress.getByName(defaultServerIP));
	}

	/**
	 * Constructs a Publisher that will connect to a specific default broker.
	 *
	 * @param defaultServerIP the IP of the default broker, interpreted as
	 *                        {@link InetAddress#getByAddress(byte[])}.
	 *
	 * @throws UnknownHostException if IP address is of illegal length
	 * @throws IOException          if an I/O error occurs when opening the
	 *                              Publisher's Server Socket.
	 */
	public Publisher(byte[] defaultServerIP) throws IOException {
		topicIPManager = new IPManager(InetAddress.getByAddress(defaultServerIP));
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	/**
	 * Pushes some Data to a Topic as a specific File Type.
	 *
	 * @param topic         the Topic to send the data to
	 * @param post          the data
	 * @param fileExtension the file type
	 */
	public void push(Topic topic, RawData post, String fileExtension) {

		Packet[] packets = Packet.dataToPackets(post, fileExtension);

		boolean success;

		do {
			InetAddress actualBrokerIP = topicIPManager.getIPForTopic(topic);

			try (Socket socket = new Socket(actualBrokerIP, Broker.PUBLIC_SERVER_PORT)) {

				PushThread pushThread;
				try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

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
				topicIPManager.invalidate(topic);
			}
		} while (!success);
	}

	@Override
	public void close() throws IOException {
		topicIPManager.close();
	}

	/**
	 * A Thread for sending some data to an output stream.
	 *
	 * @author Alex Mandelias
	 */
	private static class PushThread extends Thread {

		private final Packet[]           data;
		private final ObjectOutputStream stream;
		private boolean                  success;
		private boolean                  start, end;

		/**
		 * Constructs the Thread that, when run, will write the data to the stream
		 *
		 * @param data   the data to send
		 * @param stream the output stream to which to send the data
		 */
		public PushThread(Packet[] data, ObjectOutputStream stream) {
			this.data = data;
			this.stream = stream;
			success = false;
			start = end = false;
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
	 * update and invalidate the InetAddresses for Topics. A ServerSocket is also
	 * opened for receiving messages from brokers, which is closed by calling the
	 * {@code close()} method.
	 *
	 * @author Alex Mandelias
	 */
	private class IPManager implements AutoCloseable {
		private final Map<Topic, InetAddress> map = new HashMap<>();
		private InetAddress                   defaultBrokerIP;
		private final ServerSocket            serverSocket;

		/**
		 * Constructs the IPManager given the InetAddress to the default broker. The
		 * IPManager's server socket is also opened.
		 *
		 * @param defaultBrokerIP the IP of the default broker to connect to
		 *
		 * @throws IOException if an I/O error occurs when opening this IP Manager's
		 *                     Server Socket.
		 */
		public IPManager(InetAddress defaultBrokerIP) throws IOException {
			this.defaultBrokerIP = defaultBrokerIP;
			serverSocket = new ServerSocket(PUBLIC_SERVER_PORT);
		}

		/**
		 * Communicates with the default broker to fetch the InetAddress associated with
		 * a Topic, which is then cached. Future requests for it will use the
		 * InetAddress found in the cache.
		 * <p>
		 * To invalidate the cache and request that the default broker provide a new
		 * InetAddress, the {@link #invalidate(Topic)} method may be used.
		 *
		 * @param topic the Topic for which to get the InetAddress
		 *
		 * @return the InetAddress for that Topic
		 */
		public InetAddress getIPForTopic(Topic topic) {
			InetAddress address = map.get(topic);

			if (address != null)
				return address;

			updateIPForTopic(topic);
			return map.get(topic);
		}

		/**
		 * Invalidates the InetAddress associated with a Topic. The next time the
		 * InetAddress for said Topic is requested, the default broker will be asked
		 * will be requested to provide it.
		 *
		 * @param topic the Topic for which to invalidate the InetAddress
		 */
		public void invalidate(Topic topic) {
			map.remove(topic);
		}

		@Override
		public void close() throws IOException {
			serverSocket.close();
		}

		private void updateIPForTopic(Topic topic) {
			boolean ipForTopicBrokerException;
			do {
				ipForTopicBrokerException = false;
				try (Socket socket = getSocketToDefaultBroker();
				        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

					oos.writeObject(topic);

					InetAddress actualBrokerIPForTopic;
					try {
						actualBrokerIPForTopic = (InetAddress) ois.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}

					map.put(topic, actualBrokerIPForTopic);
				} catch (IOException e) {
					ipForTopicBrokerException = true;

					System.err
					        .printf("IOException while getting IP for Topic from default broker%n");

					boolean ipForNewDefaultBrokerException;
					do {
						ipForNewDefaultBrokerException = false;
						try (Socket socket1 = serverSocket.accept();
						        ObjectInputStream ois1 = new ObjectInputStream(
						                socket1.getInputStream())) {

							try {
								defaultBrokerIP = (InetAddress) ois1.readObject();
							} catch (ClassNotFoundException e1) {
								e1.printStackTrace();
								return;
							}

						} catch (IOException e1) {
							ipForNewDefaultBrokerException = true;

							System.err.printf(
							        "IOException while getting new InetAddress for default broker%n");
						}
					} while (ipForNewDefaultBrokerException);
				}
			} while (ipForTopicBrokerException);
		}

		private Socket getSocketToDefaultBroker() throws IOException {
			return new Socket(defaultBrokerIP, Broker.PUBLIC_SERVER_PORT);
		}
	}
}
