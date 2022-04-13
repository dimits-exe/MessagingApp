package eventDeliverySystem;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.LinkedList;

/**
 * A remote component that forms the backbone of the EventDeliverySystem.
 * Brokers act as part of a distributed server that services 
 * Publishers and Consumers.
 *
 */
class Broker implements Runnable {
	
	/**
	 * The port used by Publishers and Consumers to communicate with the Broker.
	 */
	public static final int PUBLIC_SERVER_PORT = 49672;
	
	/**
	 * The port used exclusively for inter-Broker communication.
	 */
	private static final int PRIVATE_SERVER_PORT = 49673;
	
	private static final int MAX_CONNECTIONS = 64;
	
	private final Set<Socket> clientConnections; //replace with set<InetAddress>?
	private final Set<Socket> brokerConnections;
	private final Map<Topic, LinkedList<RawData>> postsPerTopic;
	private final Map<Topic, LinkedList<RawData>> postsBackup;
	
	
	private ServerSocket clientRequestSocket;
	private ServerSocket brokerRequestSocket;
	private boolean isLeader;
	
	/**
	 * Create a new broker and add it to the already existing
	 * distributed server system.
	 * @param leaderIP the IP of the broker acting as the leader of the system
	 */
	public Broker(InetAddress leaderIP) {
		this();
		subscribeToNewBroker(leaderIP);
		isLeader = false;
	}
	
	/**
	 * Create a new broker that will act as the Leader of the
	 * distributed server.
	 */
	public Broker() {
		this.clientConnections = new HashSet<Socket>();
		this.brokerConnections = new HashSet<Socket>();
		this.postsPerTopic = new HashMap<Topic, LinkedList<RawData>>();
		this.postsBackup= new HashMap<Topic, LinkedList<RawData>>();
		isLeader = true;
	}
	
	@Override
	public void run() {
		try {
			clientRequestSocket = new ServerSocket(PUBLIC_SERVER_PORT, MAX_CONNECTIONS);
			brokerRequestSocket = new ServerSocket(PRIVATE_SERVER_PORT, MAX_CONNECTIONS);
			
			Runnable clientRequestThread = new Runnable() {

				@Override
				public void run() {
					while(true) {
						try {
							clientConnections.add(clientRequestSocket.accept());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				
			}; //clientRequestThread
			
			Runnable brokerRequestThread = new Runnable() {

				@Override
				public void run() {
					while(true) {
						try {
							brokerConnections.add(brokerRequestSocket.accept());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				
			}; //brokerRequestThread
			
			new Thread(clientRequestThread).run();
			new Thread(brokerRequestThread).run();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Add a connection from and to the new broker.
	 * @param newBrokerIP the IP of the new broker
	 */
	private void subscribeToNewBroker(InetAddress newBrokerIP) {
		try {
			brokerConnections.add(new Socket(newBrokerIP, PRIVATE_SERVER_PORT));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Send a {@link Message} redirecting the client to the
	 * broker assigned to the topic.
	 * 
	 * @param topic the topic
	 */
	private void deferToActualBroker(Topic topic) {
		if(!isLeader) {
			throw new IllegalStateException("Non-leader broker asked to redirect to other broker");
		}
		
	}
	
	/**
	 * Sends a message to all brokers.
	 * @param m the messsage
	 */
	private void multicastMessage(Message m) {
		try {
			for(Socket connection : brokerConnections) {
				ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
				out.writeObject(m);
			}
		} catch (IOException ioe) {
			System.err.println("Message transmission failed: " + ioe.toString());
		}
	}
	
	/**
	 * Sends a post to all brokers to be saved remotely.
	 * @param postPackets the packets of the post
	 */
	private void multicastPost(Packet[] postPackets) {
		try {
			for(Socket connection : brokerConnections) {
				ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
				
				for(Packet p: postPackets) {
					out.writeObject(p);
				}
				
			}
		} catch (IOException ioe) {
			System.err.println("Backup failed: " + ioe.toString());
		}
	}
	

}
