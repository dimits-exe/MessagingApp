package eventDeliverySystem.client;

import static eventDeliverySystem.datastructures.Message.MessageType.BROKER_DISCOVERY;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import eventDeliverySystem.datastructures.ConnectionInfo;
import eventDeliverySystem.datastructures.Message;
import eventDeliverySystem.util.LG;

/**
 * Wrapper for a cache that communicates with the default Broker to store,
 * update and invalidate the ConnectionInfos for Topics.
 *
 * @author Alex Mandelias
 */
class CIManager {

	private final Map<String, ConnectionInfo> map;
	private final ConnectionInfo              defaultBrokerCI;

	/**
	 * Constructs the CIManager given the ConnectionInfo to the default Broker.
	 *
	 * @param defaultBrokerConnectionInfo the ConnectionInfo of the default Broker
	 *                                    to connect to
	 */
	public CIManager(ConnectionInfo defaultBrokerConnectionInfo) {
		map = new HashMap<>();
		defaultBrokerCI = defaultBrokerConnectionInfo;
	}

	/**
	 * Communicates with the default Broker to fetch the ConnectionInfo associated
	 * with a Topic, which is then cached. Future requests for it will use the
	 * ConnectionInfo found in the cache.
	 * <p>
	 * To invalidate the cache and request that the default Broker provide a new
	 * ConnectionInfo, the {@link #invalidate(String)} method may be used.
	 *
	 * @param topicName the Topic for which to get the ConnectionInfo
	 *
	 * @return the ConnectionInfo for that Topic
	 *
	 * @throws IOException if a connection to the server fails
	 */
	public ConnectionInfo getConnectionInfoForTopic(String topicName) throws IOException {
		LG.sout("getConnectionInfoForTopic(%s)", topicName);
		LG.in();
		final ConnectionInfo address = map.get(topicName);
		LG.sout("address=%s", address);

		if (address != null)
			return address;

		updateCIForTopic(topicName);
		LG.out();
		return map.get(topicName);
	}

	/**
	 * Invalidates the ConnectionInfo associated with a Topic. The next time the
	 * ConnectionInfo for said Topic is requested, the default Broker will be asked
	 * will be requested to provide it.
	 *
	 * @param topicName the Topic for which to invalidate the ConnectionInfo
	 */
	public void invalidate(String topicName) {
		LG.sout("invalidate(%s)", topicName);
		LG.in();
		map.remove(topicName);
		LG.out();
	}

	private void updateCIForTopic(String topicName) throws IOException {
		LG.sout("updateCIForTopic(%s)", topicName);
		LG.in();
		try (Socket socket = getSocketToDefaultBroker();
		        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

			oos.writeObject(new Message(BROKER_DISCOVERY, topicName));
			oos.flush();

			try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
				ConnectionInfo actualBrokerCIForTopic;
				try {
					actualBrokerCIForTopic = (ConnectionInfo) ois.readObject();
				} catch (final ClassNotFoundException e) {
					e.printStackTrace();
					return;
				}

				LG.sout("actualBrokerCIForTopic=%s", actualBrokerCIForTopic);

				map.put(topicName, actualBrokerCIForTopic);
			}

		} catch (final IOException e) {
			throw new IOException("Fatal error: connection to default server lost", e);
		}
		LG.out();
	}

	private Socket getSocketToDefaultBroker() throws IOException {
		return new Socket(defaultBrokerCI.getAddress(), defaultBrokerCI.getPort());
	}
}
