package eventDeliverySystem;

import java.io.Serializable;
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
	//used to transmit the request for all posts via socket messages
	private static final int FETCH_ALL_POSTS = -1;

	/**
	 * A structure holding the topicName and the last post's ID of the topic in a given
	 * moment. Used to transfer the necessary information to a broker.
	 */
	class TopicToken implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String topicName;
		private final long   lastId;

		private TopicToken() {
			this.topicName = Topic.this.getName();
			this.lastId = Topic.this.getLastPost().getPostInfo().getId();
		}

		/**
		 * Returns this TopicToken's topicName.
		 *
		 * @return the topicName
		 */
		public String getName() {
			return topicName;
		}

		/**
		 * Returns this TopicToken's lastId.
		 *
		 * @return the lastId
		 */
		public long getLastId() {
			return lastId;
		}
	}

	/**
	 * Get an {@link TopicToken update token} that can be used to update the topic
	 * by the broker.
	 *
	 * @return the update token
	 */
	public TopicToken getToken() {
		return new TopicToken();
	}

	private final String name;
	private final Stack<Post> posts;

	/**
	 * Creates a new empty Topic.
	 *
	 * @param name the Topic's unique name
	 */
	public Topic(String name) {
		this.name = name;
		this.posts = new Stack<>();
	}

	/**
	 * Returns this Topic's topicName.
	 *
	 * @return the topicName
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
	 * Get the last posted post in the topic.
	 * @return the latest post
	 */
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

	/**
	 * Get all the posts in the topic.
	 * @return a full copy of the posts in the topic
	 */
	public List<Post> getAllPosts() {
		return getPostsSince(Topic.FETCH_ALL_POSTS);
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
