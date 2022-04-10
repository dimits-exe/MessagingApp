package eventDeliverySystem;

import java.util.Stack;

/**
 * A data structure holding information about a Conversation/Topic among users.
 * The structure only holds information about the contents of the conversation; it
 * doesn't track users and subscribers.
 *
 */
class Conversation {
	private final String name;
	private final int id;
	private final Stack<RawData> posts;
	
	/**
	 * Construct a new Conversation with a name and a unique id.
	 * @param name the name with which the conversation will be displayed
	 * @param id a unique identifier for the conversation
	 */
	public Conversation(String name, int id) {
		super();
		this.name = name;
		this.id = id;
		this.posts = new Stack<RawData>();
	}
	
	public String getName() {
		return name;
	}
	
	public int getId() {
		return id;
	}
	
	public Stack<RawData> getPosts() {
		Stack<RawData> newStack = new Stack<RawData>();
		newStack.addAll(posts);
		return newStack;
	}
	
	public void post(RawData postData) {
		posts.add(postData);
	}
}
