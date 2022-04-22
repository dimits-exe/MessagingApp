package eventDeliverySystem;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

/**
 * A data structure holding the contents of a conversation/topic between users.
 * Note that it does NOT keep track of users subscribed to it.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
class Topic {
	private static final int special = -1;

	private final String name;
	private final Stack<Post> posts;

	/**
	 * Creates a new empty Topic.
	 *
	 * @param name the Topic's unique name.
	 */
	public Topic(String name) {
		this.name = name;
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
	 * Add a new post to the conversation.
	 *
	 * @param postData a Post object containing the post's data.
	 */
	public void post(Post postData) {
		posts.add(postData);
	}

	/**
	 * Get a list containing posts posted after the provided postId.
	 *
	 * @param lastPostId the id of the last post saved in the conversation
	 *
	 * @return the new posts sorted last-to-first posted
	 *
	 * @throws NoSuchElementException if the lastPostId doesn't correspond to a post
	 *                                in the topic
	 */
	public List<Post> getPostsSince(long lastPostId) throws NoSuchElementException {

		Stack<Post> postsClone = new Stack<>();
		postsClone.addAll(posts);

		if (lastPostId == Topic.special) {
			return new LinkedList<>(postsClone);
		}

		LinkedList<Post> newPosts = new LinkedList<>();
		try {

			do {
				newPosts.add(postsClone.pop());
			} while (postsClone.peek().getPostInfo().getId() != lastPostId);

		} catch(EmptyStackException e) {
			throw new NoSuchElementException("There is no post with this idlol wtf");
		}

		return newPosts;
	}

	@Override
	public int hashCode() {
		try {
			return MessageDigest.getInstance("md5").digest(name.getBytes(), 0, 0);
		} catch (DigestException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
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
		return name.equals(other.name);
	}
}
