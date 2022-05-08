package eventDeliverySystem;

import java.io.Serializable;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

/**
 * Encapsulates the contents of a conversation / Topic.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
class Topic extends AbstractTopic {

	/** Constant to be used when no post exists and an ID is needed */
	static final long FETCH_ALL_POSTS = -1L;

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
			this.lastId = topic.getLastPostId();
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
	 */
	public TopicToken getToken() {
		return new TopicToken(this);
	}

	private final Stack<Post> postStack;

	/**
	 * Creates a new, empty, Topic.
	 *
	 * @param name the Topic's unique name
	 */
	public Topic(String name) {
		super(name);
		this.postStack = new Stack<>();
	}

	/**
	 * Creates a new Topic with some Posts.
	 *
	 * @param name  the Topic's unique name
	 * @param posts the Posts to add to the Topic
	 */
	public Topic(String name, Collection<Post> posts) {
		this(name);
		post(posts);
	}

	/**
	 * Returns the ID of the most recent post in this Topic.
	 *
	 * @return the most recent Post's ID or {@link Topic#FETCH_ALL_POSTS} if there
	 *         are no Posts in this Topic
	 */
	public long getLastPostId() {
		if (postStack.isEmpty())
			return Topic.FETCH_ALL_POSTS;

		return postStack.peek().getPostInfo().getId();
	}

	private final List<Packet> currPackets = new LinkedList<>();
	private PostInfo           currPI;

	@Override
	public void postHook(PostInfo postInfo) {
		if (!currPackets.isEmpty())
			throw new IllegalStateException("Recieved PostInfo while more Packets remain");

		currPI = postInfo;
	}

	@Override
	public void postHook(Packet packet) {
		currPackets.add(packet);

		if (packet.isFinal()) {
			Packet[] data = currPackets.toArray(new Packet[currPackets.size()]);
			post(Post.fromPackets(data, currPI));
			currPackets.clear();
		}
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
	 * Adds a Post to this Topic.
	 *
	 * @param post the Post
	 */
	private void post(Post post) {
		postStack.push(post);
	}

	/**
	 * TODO
	 * <p>
	 * <b>retains latest</b>
	 */
	public void clear() {
		Post first = postStack.pop();
		postStack.clear();
		postStack.push(first);
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

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		return (obj instanceof Topic);
	}
}
