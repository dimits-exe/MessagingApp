package com.example.messagingapp.eventDeliverySystem.datastructures;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.example.messagingapp.eventDeliverySystem.util.LG;

/**
 * Encapsulates the contents of a conversation / Topic.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
public class Topic extends AbstractTopic {

	private static final Post dummyPost;

	static {
		PostInfo dummyPI = new PostInfo(null, null, AbstractTopic.FETCH_ALL_POSTS);
		dummyPost = new Post(null, dummyPI);
	}

	/**
	 * Returns a token that can be used to smartly update the topic by the Broker.
	 *
	 * @return the update token
	 */
	public TopicToken getToken() {
		return new TopicToken(this);
	}

	// first element is the first post added
	private final List<Post>         postList;
	private final Map<Long, Integer> indexPerPostId;

	private Post lastPost; // don't traverse the entire postList

	/**
	 * Creates a new, empty, Topic.
	 *
	 * @param name the Topic's unique name
	 */
	public Topic(String name) {
		this(name, new LinkedList<>());
	}

	/**
	 * Creates a new Topic with some Posts.
	 *
	 * @param name  the Topic's unique name
	 * @param posts the Posts to add to the Topic
	 */
	public Topic(String name, List<Post> posts) {
		super(name);
		postList = new LinkedList<>();
		indexPerPostId = new HashMap<>();
		lastPost = null;
		post(dummyPost);

		post(posts);
	}

	/**
	 * Returns the ID of the most recent post in this Topic.
	 *
	 * @return the most recent Post's ID or {@link AbstractTopic#FETCH_ALL_POSTS} if
	 *         there are no Posts in this Topic
	 */
	public long getLastPostId() {
		return lastPost.getPostInfo().getId();
	}

	private final List<Packet> currPackets = new LinkedList<>();
	private PostInfo           currPI;

	@Override
	public void postHook(PostInfo postInfo) {
		if (!currPackets.isEmpty() || (currPI != null))
			throw new IllegalStateException("Recieved PostInfo while more Packets remain");

		currPI = postInfo;
	}

	@Override
	public void postHook(Packet packet) {
		currPackets.add(packet);

		if (packet.isFinal()) {
			final Packet[] data          = currPackets.toArray(new Packet[0]);
			final Post     completedPost = Post.fromPackets(data, currPI);
			post(completedPost);

			currPackets.clear();
			currPI = null;
		}
	}

	/**
	 * Adds a list of Posts to this Topic.
	 *
	 * @param posts the Posts
	 */
	public void post(List<Post> posts) {
		for (final Post post : posts)
			post(post);
	}

	private void post(Post post) {
		postList.add(post);
		indexPerPostId.put(post.getPostInfo().getId(), postList.size() - 1);
		lastPost = post;
	}

	/** Clears this Topic by removing all Posts */
	public void clear() {
		indexPerPostId.clear();
		postList.clear();
		post(dummyPost);
	}

	/**
	 * Returns the Posts in this Topic that were posted after the Post with the
	 * given ID. The Post with the given ID is not returned.
	 *
	 * @param lastPostId the ID of the Post.
	 *
	 * @return the Posts in this Topic that were posted after the Post with the
	 *         given ID, sorted from earliest to latest
	 *
	 * @throws NoSuchElementException if no Post in this Topic has the given ID
	 */
	public List<Post> getPostsSince(long lastPostId) throws NoSuchElementException {
		LG.sout("Topic#getPostsSince(%d)", lastPostId);
		LG.in();

		final Integer index = indexPerPostId.get(lastPostId);

		if (index == null)
			throw new NoSuchElementException(
			        "No post with id " + lastPostId + " found in this Topic");

		final List<Post> postsAfterGivenPost = new LinkedList<>(
		        postList.subList(index + 1, postList.size()));
		LG.sout("postsAfterGivenPost=%s", postsAfterGivenPost);
		LG.out();
		return postsAfterGivenPost;
	}

	/**
	 * Returns all Posts in this Topic.
	 *
	 * @return the Posts in this Topic, sorted from earliest to latest
	 */
	public List<Post> getAllPosts() {
		return getPostsSince(AbstractTopic.FETCH_ALL_POSTS);
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

	/**
	 * Encapsulates a Token that uniquely identifies a Post in a Topic and is used
	 * to transfer only the necessary information between the server and the client.
	 *
	 * @author Alex Mandelias
	 * @author Dimitris Tsirmpas
	 */
	public static class TopicToken implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String topicName;
		private final long   lastId;

		private TopicToken(Topic topic) {
			topicName = topic.getName();
			lastId = topic.getLastPostId();
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
}
