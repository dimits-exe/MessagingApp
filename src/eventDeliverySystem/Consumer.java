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
	private final Map<String, Socket> topicBrokerMap;
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
		topicBrokerMap = new HashMap<>();
		
		for(Topic t : user.getSubscribedTopics()) {
			topics.put(t.getName(), t);
		}
		//TODO: Fill topics with saved posts from disk
		
		connectionSetup();
	}

	@Override
	public void run() {
		// this probably should go to the constructor
	}

	/**
	 * Updates the local Topic with new posts streamed by a
	 * remote Topic.
	 *
	 * @param topicName the name of the Topic from which to pull
	 */
	public void pull(String topicName) {

		boolean success;
		Topic relevantTopic = topics.get(topicName);

		do {
			ConnectionInfo actualBrokerCI = topicCIManager.getConnectionInfoForTopic(topicName); // ???

			try (Socket socket = new Socket(actualBrokerCI.getAddress(), actualBrokerCI.getPort())) {

				try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
					
					// send the token of the topic to the broker
					oos.writeObject(new Message(DATA_PACKET_RECEIVE, relevantTopic.getToken()));
					
					// begin downloading the posts
					IterativePullThread pullThread = new IterativePullThread(ois, relevantTopic);
					pullThread.start();
					try {
						pullThread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				//success = pullThread.success();
				success = true; // TODO: we'll see how we can do this without repeating the same success() code for every class
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
	
	private void connectionSetup() {
		// establish connection to default server
		Socket defaultBrokerSex = null;
		
		// get broker info from default broker
		try (ObjectInputStream oos = new ObjectInputStream(defaultBrokerSex.getInputStream())) {
			Map<String, ConnectionInfo> brokerInfo = (Map<String, ConnectionInfo>) oos.readObject();
			
			// establish connections with each broker
			for(var entry : brokerInfo.entrySet()) {
				ConnectionInfo ci = entry.getValue();
				
				try {
					topicBrokerMap.put(entry.getKey(), new Socket(ci.getAddress(), ci.getPort()));
				} catch(IOException ioe) {
					System.err.println("Failed to establish connection with topic broker " + ioe);
					System.exit(-1);
				}
				
			}
		} catch(IOException ioe) {
			System.err.println("Server connection on default broker failed " + ioe);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

}
