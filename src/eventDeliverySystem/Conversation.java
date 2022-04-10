package eventDeliverySystem;

import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A data structure holding the contents of a conversation/topic between users.
 * Note that it does NOT keep track of users subscribed to it.
 * @author user
 *
 */
class Conversation {
	private final String name;
	private final long id;
	private final Stack<RawData> posts;
	
	/**
	 * Create a new conversation.
	 * @param name the name displayed when viewing the conversation.
	 * @param id
	 */
	public Conversation(String name, int id) {
		super();
		this.name = name;
		this.id = ThreadLocalRandom.current().nextLong();
		this.posts = new Stack<RawData>();
	}
	
	/**
	 * Get the name of the conversation.
	 * @return the name of the conversation.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Get a unique identifier for the conversation.
	 * @return a long int identifier
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * Get a stack containing the posts currently in the conversation.
	 * @return the posts in raw data form
	 */
	public Stack<RawData> getPosts() {
		Stack<RawData> newStack = new Stack<RawData>();
		newStack.addAll(posts);
		return newStack;
	}
	
	/**
	 * Add a new post to the conversation.
	 * @param postData a RawData object containing the post's data.
	 */
	public void post(RawData postData) {
		posts.add(postData);
	}
}
