package com.example.messagingapp.eventDeliverySystem.client;

import static com.example.messagingapp.eventDeliverySystem.datastructures.Message.MessageType.BROKER_DISCOVERY;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.example.messagingapp.eventDeliverySystem.datastructures.ConnectionInfo;
import com.example.messagingapp.eventDeliverySystem.datastructures.Message;
import com.example.messagingapp.eventDeliverySystem.server.ServerException;

/**
 * Wrapper for a cache that communicates with the default Broker to obtain and
 * store the ConnectionInfos for many Topics.
 *
 * @author Alex Mandelias
 */
class CIManager {

	private final Map<String, ConnectionInfo> cache;

	private final InetAddress defaultBrokerIP;
	private final int         defaultBrokerPort;

	/**
	 * Constructs the CIManager given the ConnectionInfo to the default Broker.
	 *
	 * @param defaultBrokerIP   the InetAddress of the default Broker to connect to
	 * @param defaultBrokerPort the Port of the default Broker to connect to
	 */
	public CIManager(InetAddress defaultBrokerIP, int defaultBrokerPort) {
		cache = new HashMap<>();
		this.defaultBrokerIP = defaultBrokerIP;
		this.defaultBrokerPort = defaultBrokerPort;
	}

	/**
	 * Communicates with the default Broker to fetch the ConnectionInfo associated
	 * with a Topic, which is then cached. Future requests for it will use the
	 * ConnectionInfo found in the cache.
	 *
	 * @param topicName the Topic for which to get the ConnectionInfo
	 *
	 * @return the ConnectionInfo for that Topic
	 *
	 * @throws ServerException if a connection to the server fails
	 */
	public ConnectionInfo getConnectionInfoForTopic(String topicName) throws ServerException {
		ConnectionInfo address = cache.get(topicName);
		if (address != null)
			return address;

		address = getCIForTopic(topicName);
		cache.put(topicName, address);
		return address;
	}

	private ConnectionInfo getCIForTopic(String topicName) throws ServerException {
		try (Socket socket = new Socket(defaultBrokerIP, defaultBrokerPort)) {

			final ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(new Message(BROKER_DISCOVERY, topicName));
			oos.flush();
			final ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

			ConnectionInfo actualBrokerCIForTopic;
			try {
				actualBrokerCIForTopic = (ConnectionInfo) ois.readObject();
			} catch (final ClassNotFoundException e) {
				e.printStackTrace();
				actualBrokerCIForTopic = null;
			}

			return actualBrokerCIForTopic;

		} catch (final IOException e) {
			throw new ServerException(e);
		}
	}
}
