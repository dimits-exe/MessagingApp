package eventDeliverySystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eventDeliverySystem.Message.MessageType;

/**
 * A remote component that forms the backbone of the EventDeliverySystem.
 * Brokers act as part of a distributed server that services
 * Publishers and Consumers.
 *
 */
class Broker implements Runnable {

	private static final PortManager portManager = new PortManager();
	private static final int MAX_CONNECTIONS = 64;
	

	private final Set<ConnectionInfo> clientInfo;
	
	// === PLACEHOLDERS ===
	
	private final List<Socket> brokerConnections;
	private final Map<Topic, LinkedList<Post>> postsPerTopic;
	private final Map<Topic, LinkedList<Post>> postsBackup;

	private boolean isLeader;

	// === END PLACEHOLDERS ===

	private ServerSocket clientRequestSocket;
	private ServerSocket brokerRequestSocket;
	
	
	/**
	 * Create a new broker and add it to the already existing
	 * distributed server system.
	 * @param leaderIP the IP of the broker acting as the leader of the system
	 */
	public Broker(InetAddress leaderIP) {
		this();
		isLeader = false;
	}

	/**
	 * Create a new broker that will act as the Leader of the
	 * distributed server.
	 */
	public Broker() {
		//TODO: establish connection with broker
		this.clientInfo = Collections.synchronizedSet(new HashSet<>());
		this.brokerConnections = Collections.synchronizedList(new LinkedList<>());
		this.postsPerTopic = Collections.synchronizedMap(new HashMap<>());
		this.postsBackup = Collections.synchronizedMap(new HashMap<>());
		isLeader = true;
	}

	@Override
	public void run() {
		try {
			clientRequestSocket = new ServerSocket(portManager.getNewAvailablePort(), MAX_CONNECTIONS);
			brokerRequestSocket = new ServerSocket(portManager.getNewAvailablePort(), MAX_CONNECTIONS);
			
			// Start handling client requests
			Runnable clientRequestThread = () -> {
                while (true) {
                    try {
                        Socket socket = clientRequestSocket.accept();
                        new ClientRequestHandler(socket).start();

                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1); 		// serious error when waiting, close broker
                    }
                }
            }; //clientRequestThread
            
            
            //TODO: properly implement
			Runnable brokerRequestThread = new Runnable() {

				@Override
				public void run() {
					while(true) {
						try {
							brokerConnections.add(brokerRequestSocket.accept());
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(-1); 	// serious error when waiting, close broker
						}
					}
				}

			}; //brokerRequestThread

			new Thread(clientRequestThread).start();
			new Thread(brokerRequestThread).start();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	private Thread threadFactory(Message message, Socket connection) throws IOException {
		switch(message.getType()) {
		case DATA_PACKET:
			PostInfo pi = (PostInfo) message.getValue();
			return new PullThread(new ObjectInputStream(connection.getInputStream()), pi);

		case DISCOVER:
			Topic t = (Topic) message.getValue();
			return new DiscoveryThread(new ObjectOutputStream(connection.getOutputStream()), t);
			
		default:
			throw new IllegalArgumentException("How the fuck");
		}
	}
	
	// ============== UNUSED =======================
	
	/**
	 * Sends a message to all brokers.
	 * @param m the messsage
	 */
	private void multicastMessage(Message m) {
		
		for(Socket connection : brokerConnections) {
			try(ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream())) {
				out.writeObject(m);
			} catch (IOException ioe) {
				System.err.println("Message transmission failed: " + ioe.toString());
			}
		}
		
	}
	
	/**
	 * Return the broker that's responsible for the requested topic.
	 * @param topic the topic
	 * @return the {@link ConnectionInfo} of the assigned broker
	 */
	private ConnectionInfo getAssignedBroker(Topic topic) {
		int brokerIndex = topic.hashCode() % brokerConnections.size();
		final Socket broker = brokerConnections.get(brokerIndex);
		return new ConnectionInfo(broker.getInetAddress(), broker.getPort());
	}
	
	/**
	 * Update internal data structures once a new connection has been established with
	 * another broker.
	 * 
	 * @param newBroker the information of the connected broker
	 */
	private void newBrokerConnected(Socket newBroker) {
		if(brokerConnections.contains(newBroker))
			return;
		
		brokerConnections.add(newBroker);
		brokerConnections.sort(new Comparator<Socket>() {
			
			/**
			 * Compares IP addresses and if equal, with ports.
			 */
			@Override
			public int compare(Socket s1, Socket s2) {
				
				int ipc =s1.getInetAddress().getHostName().compareTo(s2.getInetAddress().getHostName());
				
				if(ipc == 0) {
					return s2.getPort() - s1.getPort();
				} else {
					return ipc;
				}
					
			}
		
		}); //sort
	}
	
	
	/**
	 * Get the assigned topics for this broker.
	 * @return the topics
	 */
	private Set<Topic> getAssignedTopics() {
		return this.postsPerTopic.keySet();
	}
	

	 private class ClientRequestHandler extends Thread {
	
        private Socket socket;

        public ClientRequestHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
        	
			try(ObjectInputStream ois 	= new ObjectInputStream(socket.getInputStream())) {
				Message           m   	= (Message) ois.readObject();
				Thread thread 			= Broker.this.threadFactory(m, socket);
				thread.start();
				
				Broker.this.clientInfo.add(new ConnectionInfo(socket.getInetAddress(), socket.getPort()));
			} catch (IOException ioe) {
				// do nothing
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

           
        }
    }
	
	/**
	 * A Thread for receiving some data from an input stream.
	 *
	 * @author Alex Mandelias
	 */
	private class PullThread extends Thread {

		private final ObjectInputStream stream;
		private final PostInfo postInfo;

		/**
		 * Constructs the Thread that, when run, will read data from the stream.
		 *
		 * @param stream the input stream from which to read the data
		 */
		public PullThread(ObjectInputStream stream, PostInfo postInfo) {
			this.stream = stream;
			this.postInfo = postInfo;
		}

		@Override
		public void run() {

			final List<Packet> postFragments = new LinkedList<>();

			try {
				Packet packet;
				do {
					try {
						packet = (Packet) stream.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}
					postFragments.add(packet);
				} while (!packet.isFinal());

				final Packet[] packets = postFragments.toArray(new Packet[postFragments.size()]);
				final Post     post    = Post.fromPackets(packets, this.postInfo);
				postsPerTopic.get(post.getPostInfo().getTopicName()).add(post);

				// TODO: multicast this post to every broker

			} catch (IOException e) {
				// do nothing
			}
		}
	}

	/**
	 * A Thread for discovering the actual broker for a topic.
	 *
	 * @author Alex Mandelias
	 */
	private class DiscoveryThread extends Thread {

		private final ObjectOutputStream stream;
		private final Topic              topic;

		/**
		 * Constructs the Thread that, when run, will write the address of the broker
		 * that has the requested topic in the given output stream.
		 *
		 * @param stream  the output stream to which to write the data
		 * @param topic the topic 
		 */
		public DiscoveryThread(ObjectOutputStream stream, Topic topic) {
			this.stream = stream;
			this.topic = topic;
		}

		@Override
		public void run() {

			try {
				ConnectionInfo brokerInfo = Broker.this.getAssignedBroker(topic);
				stream.writeObject(brokerInfo);
			} catch (IOException e) {
				// do nothing
			}
		}
	}


	
}
