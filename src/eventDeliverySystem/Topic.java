package eventDeliverySystem;

import java.math.BigInteger;
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
	private static final int FETCH_ALL_POSTS = -1;

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
	 * Returns this Topic's name.
	 *
	 * @return the name
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
	
	public Post getLastPost() {
		return posts.peek();
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

		if (lastPostId == Topic.FETCH_ALL_POSTS) {
			return new LinkedList<>(postsClone);
		}

		LinkedList<Post> newPosts = new LinkedList<>();
		try {

			do {
				newPosts.add(postsClone.pop());
			} while (postsClone.peek().getPostInfo().getId() != lastPostId);

		} catch(EmptyStackException e) {
			throw new NoSuchElementException(
			        "No post with id " + lastPostId + " found in the stack");
		}

		return newPosts;
	}

	@Override
	public int hashCode() {
		try {
			MessageDigest a = MessageDigest.getInstance("md5");
			byte[]        b = a.digest(name.getBytes());

			// big brain stuff
			final int FOUR = 4;
			int       c    = FOUR;
			int       d    = b.length / c;
			byte[]    e    = new byte[c];
			for (int f = 0; f < e.length; f++)
				for (int g = 0; g < d; g++)
					e[f] ^= (b[(d * f) + g]);

			BigInteger h = new BigInteger(e);
			return h.intValueExact();

		} catch (NoSuchAlgorithmException | ArithmeticException e) {
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
