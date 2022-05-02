package eventDeliverySystem;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Stack;

/**
 * Encapsulates the contents of a conversation / Topic.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
class Topic {

	// used to transmit the request for all posts via socket messages
	private static final int FETCH_ALL_POSTS = -1;

	/**
	 * Encapsulates a Token that uniquely identifies a Post in a Topic and is used
	 * to transfer only the necessary information between the server and the client.
	 *
	 * @author Alex Mandelias
	 * @author Dimitris Tsirmpas
	 */
	static class TopicToken implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String topicName;
		private final long   lastId;

		private TopicToken(Topic topic) {
			this.topicName = topic.getName();
			long tempId;
			try {
				tempId = topic.postStack.peek().getPostInfo().getId();
			} catch (EmptyStackException e) {
				tempId = Topic.FETCH_ALL_POSTS;
			}
			this.lastId = tempId;
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
	 * Get an update token that can be used to smartly update the topic by the
	 * broker.
	 *
	 * @return the update token
	 *
	 * @see TopicToken
	 */
	public TopicToken getToken() {
		return new TopicToken(this);
	}

	private final String name;
	private final Stack<Post> postStack;

	/**
	 * Creates a new, empty, Topic.
	 *
	 * @param name the Topic's unique name
	 */
	public Topic(String name) {
		this.name = name;
		this.postStack = new Stack<>();
	}

	/**
	 * Creates a new Topic with some Posts.
	 *
	 * @param name  the Topic's unique name
	 * @param posts the Posts to add to the Topic
	 */
	public Topic(String name, List<Post> posts) {
		this(name);
		post(posts);
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
	 * Adds a Post to this Topic.
	 *
	 * @param post the Post
	 */
	public void post(Post post) {
		postStack.push(post);
	}

	/**
	 * Adds a collection of Posts to this Topic.
	 *
	 * @param posts the Posts
	 */
	public void post(Collection<Post> posts) {
		for (Post post : posts)
			post(post);
	}

	/**
	 * Returns the ID of the most recent Post in this Topic.
	 *
	 * @return the ID of the most recent Post or {@code -1} if there are no Posts in
	 *         this Topic
	 */
	public long getLastPostId() {
		if (postStack.isEmpty())
			return -1;

		return postStack.peek().getPostInfo().getId();
	}

	/**
	 * Returns the Posts in this Topic that were posted after the Post with the
	 * given ID. The Post with the given ID is not returned.
	 *
	 * @param lastPostId the ID of the Post
	 *
	 * @return the Posts in this Topic that were posted after the Post with the
	 *         given ID, sorted from latest to earliest
	 *
	 * @throws NoSuchElementException if no Post in this Topic has the given ID
	 */
	public List<Post> getPostsSince(long lastPostId) throws NoSuchElementException {
		LG.sout("Topic#getPostsSince(%d)", lastPostId);
		LG.in();

		Stack<Post> postsClone = new Stack<>();
		postsClone.addAll(postStack);

		LG.sout("postsClone=%s", postsClone);

		LinkedList<Post> postsAfterGivenPost = new LinkedList<>();

		if (lastPostId == Topic.FETCH_ALL_POSTS) {
			while (!postsClone.isEmpty())
				postsAfterGivenPost.add(postsClone.pop());

			LG.sout("postsAfterGivenPost=%s", postsAfterGivenPost);

			LG.out();
			return postsAfterGivenPost;
		}

		try {
			while (postsClone.peek().getPostInfo().getId() != lastPostId)
				postsAfterGivenPost.add(postsClone.pop());

			LG.out();
			return postsAfterGivenPost;
		} catch (EmptyStackException e) {
			throw new NoSuchElementException(
			        "No post with id " + lastPostId + " found in the stack");
		}
	}

	/**
	 * Returns all Posts in this Topic.
	 *
	 * @return the Posts in this Topic, sorted from latest to earliest
	 */
	public List<Post> getAllPosts() {
		return getPostsSince(Topic.FETCH_ALL_POSTS);
	}

	/**
	 * Returns the hash that a Topic with a given name would have. Since a Topic's
	 * hash is determined solely by its name, this method returns the same result as
	 * Topic#hashCode(), when given the name of the Topic, and can be used when an
	 * instance of Topic is not available, but its name is known.
	 *
	 * @param topicName the name of the Topic for which to compute the hash
	 *
	 * @return a hash code value for this Topic
	 */
	public static int hashForTopic(String topicName) {
		try {
			MessageDigest a = MessageDigest.getInstance("md5");
			byte[]        b = a.digest(topicName.getBytes());

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
	public int hashCode() {
		return Topic.hashForTopic(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Topic))
			return false;
		Topic other = (Topic) obj;
		return Objects.equals(name, other.name); // same name == same Topic, can't have duplicate names
	}
}
