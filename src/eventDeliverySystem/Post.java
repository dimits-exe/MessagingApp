package eventDeliverySystem;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * Contains information about a Post, its data and its associated PostInfo.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 *
 * @see PostInfo
 */
class Post {

	public enum DataType {
		TEXT, VIDEO, IMAGE
	}

	/**
	 * Extracts the data of some Packets into a single RawData object.
	 *
	 * @param packets an array of packets that stores the entire contents of a
	 *                RawData object
	 *
	 * @return the packets' contents as a RawData object
	 *
	 * @throws IllegalArgumentException if the packet array has length 0
	 * @throws IllegalStateException    if packets with different ID are found in
	 *                                  the array
	 */
	public static Post fromPackets(Packet[] packets, PostInfo postInfo) {
		if (packets.length == 0)
			throw new IllegalArgumentException("Tried to create a RawData object with no data");

		final byte[] data = new byte[Stream.of(packets).mapToInt(p -> p.getPayload().length).sum()];
		int          ptr  = 0;

		final long idOfFirst = packets[0].getPostId();

		for (int i = 0, count = packets.length; i < count; i++) {
			final Packet curr = packets[i];

			if (curr.getPostId() != idOfFirst)
				throw new IllegalStateException(String.format(
				        "Tried to combine packet with id %d with packet with id %d", idOfFirst,
				        curr.getPostId()));

			final byte[] payload = curr.getPayload();
			final int    length  = payload.length;
			System.arraycopy(payload, 0, data, (ptr += length) - length, length);
		}

		return new Post(data, postInfo);
	}

	private final byte[]   data;
	private final PostInfo postInfo;

	/**
	 * Constructs a new Post with a random ID.
	 *
	 * @param data          the data to be encapsulated in the Post
	 * @param type          the Type of the data of the Post
	 * @param posterId      the ID of the Poster of the Post
	 * @param fileExtension the file extension associated with the data of the Post.
	 *                      Must be {@code null} if {@code type == DataType.TEXT}.
	 * @param topicName     the Topic of the Post
	 */
	public Post(byte[] data, DataType type, long posterId, String fileExtension,
	        String topicName) {
		this(data, type, posterId, fileExtension, topicName,
		        ThreadLocalRandom.current().nextLong());
	}

	/**
	 * Constructs a new Post with the specified info.
	 *
	 * @param data the contents of the post
	 * @param postInfo the info of the post
	 */
	public Post(byte[] data, PostInfo postInfo) {
		this(data, postInfo.getType(), postInfo.getPosterId(), postInfo.getFileExtension(),
		        postInfo.getTopicName(),
				postInfo.getId());
	}

	/**
	 * Constructs a new Post with a set ID.
	 *
	 * @param data          the data to be encapsulated in the Post
	 * @param type          the Type of the data of the Post
	 * @param posterId      the ID of the Poster of the Post
	 * @param fileExtension the file extension associated with the data of the Post.
	 *                      Must be {@code null} if {@code type == DataType.TEXT}.
	 * @param topicName     the Topic of the Post
	 * @param postID        the id of the Post
	 */
	private Post(byte[] data, DataType type, long posterId, String fileExtension, String topicName,
	        long postID) {
		if ((type == DataType.TEXT) && (fileExtension != null))
			throw new IllegalArgumentException("DataType TEXT requires 'null' file extension");

		this.data = data;
		this.postInfo = new PostInfo(type, posterId, fileExtension, topicName, postID);
	}

	/**
	 * Returns a clone of the data.
	 *
	 * @return the data
	 */
	public byte[] getData() {
		return data.clone();
	}

	/**
	 * Returns this Post's postInfo.
	 *
	 * @return the postInfo
	 */
	public PostInfo getPostInfo() {
		return postInfo;
	}

	@Override
	public String toString() {
		return String.format("%d bytes: %s", data.length, postInfo);
	}
}
