package eventDeliverySystem;

import static eventDeliverySystem.Message.MessageType.DATA_PACKET_RECEIVE;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A process that holds the posts of a certain user and updates them
 * by connecting to a remote server.
 * 
 */
class Consumer extends ClientNode {

	// Local saved posts
	private final Map<String, Topic> topics;
	private final Profile user;
	
	//TODO: implement getting a list of sockets from broker
	//TODO: keep said sockets connected and choose between them (no CIManager ?)
	
	/**
	 * Constructs a Consumer that will connect to a specific default broker.
	 * 
	 * @see {@link ClientNode#UserNode(String, int)}
	 */
	public Consumer(String defaultServerIP, int defaultServerPort, Profile user) throws IOException {
		this(InetAddress.getByName(defaultServerIP), defaultServerPort, user);
	}
	
	/**
	 * Constructs a Consumer that will connect to a specific default broker.
	 * 
	 * @see {@link ClientNode#UserNode(byte[], int)}
	 */
	public Consumer(byte[] defaultServerIP, int defaultServerPort, Profile user) throws IOException {
		this(InetAddress.getByAddress(defaultServerIP), defaultServerPort, user);
	}
	
	protected Consumer(InetAddress ip, int port, Profile user) throws IOException {
		super(ip, port);
		this.user = user;
		topics = new HashMap<>();
		
		for(Topic t : user.getSubscribedTopics()) {
			topics.put(t.getName(), t);
		}
		//TODO: Fill topics with saved posts from disk
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	/**
	 * Updates the local Topic with new posts streamed by a
	 * remote Topic.
	 *
	 * @param topicName the name of the Topic from which to pull
	 */
	public void pull(String topicName) {

		boolean success;
		PullThread pullThread = null;
		Topic relevantTopic = topics.get(topicName);

		do {
			ConnectionInfo actualBrokerCI = topicCIManager.getConnectionInfoForTopic(topicName);

			try (Socket socket = new Socket(actualBrokerCI.getAddress(), actualBrokerCI.getPort())) {

				try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
					
					// get last post from the topic and send it to the broker
					long idOfLast = relevantTopic.getLastPost().getPostInfo().getId();
					oos.writeObject(new Message(DATA_PACKET_RECEIVE, idOfLast));
					
					// receive broker's answer on how many posts need to be sent
					// so the local topic is updated
					int postCount;
					try {
						postCount = (Integer) ois.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}
					
					// begin downloading the posts
					pullThread = new PullThread(ois, relevantTopic, postCount);
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

	}
	
	public List<Post> getPostsByTopic(String topicName) {
		return topics.get(topicName).getAllPosts();
	}

}
