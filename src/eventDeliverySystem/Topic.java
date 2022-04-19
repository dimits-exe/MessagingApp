package eventDeliverySystem;

import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A data structure holding the contents of a conversation/topic between users.
 * Note that it does NOT keep track of users subscribed to it.
 * @author user
 *
 */
class Topic {
	private final String name;
	private final long id;
	private final Stack<Post> posts;

	/**
	 * Create a new conversation.
	 * @param name the name displayed when viewing the conversation.
	 */
	public Topic(String name) {
		super();
		this.name = name;
		this.id = ThreadLocalRandom.current().nextLong();
		this.posts = new Stack<>();
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
	 * Get a list containing posts posted after the provided postId.
	 * @param lastPostId the id of the last post saved in the conversation
	 * @return the new posts sorted last-to-first posted
	 * @throws NoSuchElementException if the lastPostId doesn't correspond to
	 * a post in the topic
	 */
	public List<Post> getPostsSince(long lastPostId) throws NoSuchElementException {
		Stack<Post> newStack = new Stack<>();
		newStack.addAll(posts);

		LinkedList<Post> newPosts = new LinkedList<>();
		try {

			do {
				newPosts.add(newStack.pop());
			} while (newStack.peek().getPostInfo().getId() != lastPostId);

		} catch(EmptyStackException e) {
			throw new NoSuchElementException("There is no post with this idlol wtf"); //maybe just send all posts anyway
		}
		return newPosts;
	}

	/**
	 * Add a new post to the conversation.
	 *
	 * @param postData a Post object containing the post's data.
	 */
	public void post(Post postData) {
		posts.add(postData);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Topic other = (Topic) obj;
		return id == other.id;
	}
}
