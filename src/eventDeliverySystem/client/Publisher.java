package eventDeliverySystem.client;

import static eventDeliverySystem.datastructures.Message.MessageType.CREATE_TOPIC;
import static eventDeliverySystem.datastructures.Message.MessageType.DATA_PACKET_SEND;

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
import java.util.Queue;

import eventDeliverySystem.User.UserSub;
import eventDeliverySystem.datastructures.ConnectionInfo;
import eventDeliverySystem.datastructures.Message;
import eventDeliverySystem.datastructures.Packet;
import eventDeliverySystem.datastructures.Post;
import eventDeliverySystem.datastructures.PostInfo;
import eventDeliverySystem.server.Broker;
import eventDeliverySystem.thread.PushThread;
import eventDeliverySystem.thread.PushThread.Callback;
import eventDeliverySystem.thread.PushThread.Protocol;
import eventDeliverySystem.util.LG;

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

	private final UserSub           usersub;
	private final Queue<PushStruct> queue;
	private final Thread            postThread;

	/**
	 * Constructs a Publisher.
	 *
	 * @param defaultServerIP   the IP of the default broker, interpreted as
	 *                          {@link InetAddress#getByName(String)}.
	 * @param defaultServerPort the port of the default broker
	 * @param usersub           the UserSub object that will be notified if a push
	 *                          fails
	 *
	 * @throws UnknownHostException if no IP address for the host could be found, or
	 *                              if a scope_id was specified for a global IPv6
	 *                              address while resolving the defaultServerIP.
	 */
	public Publisher(String defaultServerIP, int defaultServerPort, UserSub usersub)
	        throws UnknownHostException {
		this(InetAddress.getByName(defaultServerIP), defaultServerPort, usersub);
	}

	/**
	 * Constructs a Publisher.
	 *
	 * @param defaultServerIP   the IP of the default broker, interpreted as
	 *                          {@link InetAddress#getByAddress(byte[])}.
	 * @param defaultServerPort the port of the default broker
	 * @param usersub           the UserSub object that will be notified if a push
	 *                          fails
	 *
	 * @throws UnknownHostException if IP address is of illegal length
	 */
	public Publisher(byte[] defaultServerIP, int defaultServerPort, UserSub usersub)
	        throws UnknownHostException {
		this(InetAddress.getByAddress(defaultServerIP), defaultServerPort, usersub);
	}

	/**
	 * Constructs a Publisher.
	 *
	 * @param ip      the InetAddress of the default broker
	 * @param port    the port of the default broker
	 * @param usersub the UserSub object that will be notified if a push fails
	 */
	private Publisher(InetAddress ip, int port, UserSub usersub) {
		super(ip, port);
		this.usersub = usersub;
		queue = new LinkedList<>();
		postThread = new PostThread();
		postThread.start();
	}

	/**
	 * Pushes a Post by creating a new Thread that connects to the actual Broker and
	 * starts a PushThread.
	 *
	 * @param post      the Post
	 * @param topicName the name of the Topic to which to push the Post
	 */
	public void push(Post post, String topicName) {
		LG.sout("Publisher#push(%s, %s)", post, topicName);
		LG.in();
		synchronized (queue) {
			queue.add(new PushStruct(post, topicName));
		}
		synchronized (postThread) {
			postThread.notify();
		}
		LG.out();
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
	 *
	 * @throws IOException if a connection to the server fails
	 */
	public boolean createTopic(String topicName) throws IOException {

		final ConnectionInfo actualBrokerCI = topicCIManager.getConnectionInfoForTopic(topicName);

		try (Socket socket = new Socket(actualBrokerCI.getAddress(), actualBrokerCI.getPort())) {
			final ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.flush();
			final ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

			oos.writeObject(new Message(CREATE_TOPIC, topicName));

			return ois.readBoolean(); // true or false, successful creation or not

		} catch (final IOException e) {
			throw new IOException("Fatal error: can't connect to server for topic " + topicName, e);
		}
	}

	private class PostThread extends Thread {

		private final Callback callback = (success, topicName) -> {
			if (success)
				usersub.failure(topicName);
		};

		/**
		 * Constructs the Thread that, when run, will write some Posts to a stream. This
		 * Thread is subscribed to a Topic and is notified each time there is new data
		 * in the Topic.
		 */
		public PostThread() {
			super("PostThread");
		}

		@Override
		public void run() {
			boolean isEmpty;
			while (true) {
				synchronized (queue) {
					isEmpty = queue.isEmpty();
				}

				while (isEmpty) {
					LG.sout("--- queue is empty ---");
					try {
						synchronized (this) {
							this.wait();
						}
					} catch (final InterruptedException e) {}

					synchronized (queue) {
						isEmpty = queue.isEmpty();
					}
				}

				LG.sout("--- queue is no longer empty ---");
				do {
					PushStruct ps;
					synchronized (queue) {
						ps = queue.remove();
					}

					try {
						final ConnectionInfo actualBrokerCI = topicCIManager
						        .getConnectionInfoForTopic(ps.topicName);
						LG.sout("Actual Broker CI: %s", actualBrokerCI);

						final Socket socket = new Socket(actualBrokerCI.getAddress(),
						        actualBrokerCI.getPort());

						final ObjectOutputStream oos = new ObjectOutputStream(
						        socket.getOutputStream());
						oos.writeObject(new Message(DATA_PACKET_SEND, ps.topicName));

						final PostInfo            postInfo  = ps.post.getPostInfo();
						final List<PostInfo>      postInfos = new LinkedList<>();
						final Map<Long, Packet[]> packets   = new HashMap<>();

						postInfos.add(postInfo);
						packets.put(postInfo.getId(), Packet.fromPost(ps.post));

						final PushThread pushThread = new PushThread(oos, ps.topicName, postInfos,
						        packets, Protocol.NORMAL, callback);
						pushThread.start();

					} catch (final IOException e) {
						callback.onCompletion(false, ps.topicName);
					}

					synchronized (queue) {
						isEmpty = queue.isEmpty();
					}
				} while (isEmpty);
			}
		}
	}

	private static class PushStruct {

		private final Post   post;
		private final String topicName;

		/**
		 * Creates a new PushStruct.
		 *
		 * @param post      the Post to post
		 * @param topicName the name of the Topic to which the Post will be posted
		 */
		public PushStruct(Post post, String topicName) {
			this.post = post;
			this.topicName = topicName;
		}
	}
}
