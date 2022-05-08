package eventDeliverySystem.client;

import static eventDeliverySystem.datastructures.Message.MessageType.CREATE_TOPIC;
import static eventDeliverySystem.datastructures.Message.MessageType.DATA_PACKET_SEND;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import eventDeliverySystem.Broker;
import eventDeliverySystem.LG;
import eventDeliverySystem.datastructures.ConnectionInfo;
import eventDeliverySystem.datastructures.Message;
import eventDeliverySystem.datastructures.Packet;
import eventDeliverySystem.datastructures.Post;
import eventDeliverySystem.datastructures.PostInfo;
import eventDeliverySystem.thread.PushThread;
import eventDeliverySystem.thread.PushThread.Protocol;

/**
 * A client-side process which is responsible for creating Topics and pushing
 * Posts to them by connecting to a remote server.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirbas
 *
 * @see Broker
 */
public class Publisher extends ClientNode {

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
	private Publisher(InetAddress ip, int port) throws IOException {
		super(ip, port);
	}

	/**
	 * Pushes a Post by creating a new Thread that connects to the actual Broker and
	 * starts a PushThread.
	 *
	 * @param post      the Post
	 * @param topicName the name of the Topic to which to push the Post
	 */
	public void push(Post post, String topicName) {

		// MAYBE-TODO: add queue instead of making many separate Runnable jobs to ensure
		// 1 - non-blocking push
		// 2 - synchronisation

		LG.sout("Publisher#push(%s, %s)", post, topicName);

		LG.in();
		Runnable job = () -> {

			boolean success;

			do {
				ConnectionInfo actualBrokerCI = topicCIManager
				        .getConnectionInfoForTopic(topicName);

				LG.sout("Actual Broker CI: %s", actualBrokerCI);

				try {
					Socket socket = new Socket(actualBrokerCI.getAddress(),
					        actualBrokerCI.getPort());

					PushThread pushThread;
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

					oos.writeObject(new Message(DATA_PACKET_SEND, topicName));

					PostInfo                postInfo  = post.getPostInfo();
					List<PostInfo>          postInfos = List.of(postInfo);
					Map<Long, Packet[]> packets   = Map.of(postInfo.getId(), Packet.fromPost(post));
					pushThread = new PushThread(oos, postInfos, packets, Protocol.NORMAL);
					pushThread.start();
					try {
						pushThread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					success = pushThread.success();

				} catch (IOException e) {

					System.err.printf("IOException while connecting to actual broker%n");
					e.printStackTrace();

					success = false;
					topicCIManager.invalidate(topicName);
				}
			} while (!success);
		};
		LG.out();

		new Thread(job, "PublisherWorkerThread").start();
	}

	/**
	 * Request that the remote server create a new Topic with the specified name by
	 * connecting to the actual Broker for the Topic.
	 *
	 * @param topicName the name of the new Topic
	 *
	 * @return {@code true} if Topic was successfully created, {@code false} if an
	 *         IOException occurred while transmitting the request or if a Topic
	 *         with that name already exists
	 */
	public boolean createTopic(String topicName) {

		ConnectionInfo actualBrokerCI = topicCIManager.getConnectionInfoForTopic(topicName);
		boolean        success;

		try (Socket socket = new Socket(actualBrokerCI.getAddress(), actualBrokerCI.getPort())) {
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.flush();
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

			oos.writeObject(new Message(CREATE_TOPIC, topicName));
			success = ois.readBoolean();

		} catch (IOException e) {
			System.err.printf("IOException while creating Topic '%s'%n", topicName);
			e.printStackTrace();
			topicCIManager.invalidate(topicName);
			success = false;
		}

		return success;
	}
}
