package eventDeliverySystem;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.List;

	
/**
 * A class that manages the actions of the user by communicating with the server
 * and retrieving / committing posts to the file system.
 * 
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
public class User {
	
	private final Publisher publisher;
	private final Consumer consumer;
	private final Path userDir;
	private final Profile user;
	
	/**
	 * Retrieve the user's data and the saved posts, establish connection to the server
	 * and prepare to receive and send posts.
	 * @param serverIP the IP of the server
	 * @param port the port of the server
	 * @param userDir the base directory of all the saved posts and topics
	 * @throws IOException if a connection can't be established with the server or
	 * if the file system can't be reached
	 */
	public User(InetAddress serverIP, int port, Path userDir) throws IOException {
		this.userDir = userDir;
		
		// load user data
		try {
			this.user = loadUserData();
		} catch(IOException ioe) {
			throw new IOException("Could not load user data.", ioe);
		}
		
		// create user processes
		try {
			this.publisher = new Publisher(serverIP, port);
			this.consumer = new Consumer(serverIP, port, user.getSubscribedTopics());
		} catch(IOException ioe) {
			throw new IOException("Could not establish connection with server.", ioe);
		}
		
	}
	
	/**
	 * Commit all new posts to the File System.
	 * @param topicName the name of the topic
	 * @throws IOException if the connection with the server is interrupted 
	 * or if the File System is unreachable
	 */
	public void pull(String topicName) throws IOException {
		List<Post> posts = consumer.pull(topicName);
		
		for(Post post : posts) {
			savePost(post);
		}
	}
	
	/**
	 * Request the remote server to create a new topic with the specified name.
	 * @param topicName the name of the new topic
	 */
	public void createTopic(String TopicName) {
		publisher.createTopic(TopicName);
	}
	
	/**
	 * Send a new post to the server.
	 * @param post the new post
	 * @throws IOException if the connection with the server is interrupted
	 */
	public void post(Post post) throws IOException {
		publisher.push(post);
		savePost(post);
	}
	
	/**
	 * Get the directory used to save the application's posts and data.
	 * @return the user directory
	 */
	public Path getUserDirectory() {
		return userDir;
	}
	
	/**
	 * Commit a new Post to the File System.
	 * @param post the post to be saved
	 * @throws IOException if the File System is unreachable
	 */
	private void savePost(Post post) throws IOException {
		//TODO: implement
	}
	
	
	
	private Profile loadUserData() throws IOException {
		//TODO: implement
		return null;
	}

}
