package eventDeliverySystem;

import java.util.Stack;

class Conversation {
	private final String name;
	private final int id;
	private final Stack<RawData> posts;
	
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
	
	@SuppressWarnings("unchecked")
	public Stack<RawData> getPosts() {
		return (Stack<RawData>) posts.clone();
	}
	
	public void post(RawData postData) {
		posts.add(postData);
	}
}
>>>>>>> branch 'init' of https://github.com/dimits-exe/MessagingApp
