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
	public static Post fromPackets(Packet[] packets) {
		if (packets.length == 0)
			throw new IllegalArgumentException("Tried to create a RawData object with no data");

		final byte[] data = new byte[Stream.of(packets).mapToInt(p -> p.getPayload().length).sum()];
		int          ptr  = 0;

		final long idOfFirst = packets[0].getPostInfo().getId();

		for (int i = 0, count = packets.length; i < count; i++) {
			final Packet curr = packets[i];

			if (curr.getPostInfo().getId() != idOfFirst)
				throw new IllegalStateException(String.format(
				        "Tried to combine packet with id %d with packet with id %d", idOfFirst,
				        curr.getPostInfo().getId()));

			final byte[] payload = curr.getPayload();
			final int    length  = payload.length;
			System.arraycopy(payload, 0, data, (ptr += length) - length, length);
		}

		final PostInfo p = packets[0].getPostInfo();
		return new Post(data, p.getType(), p.getPoster(), p.getFileExtension(), p.getTopic(),
		        p.getId());
	}

	private final byte[]   data;
	private final PostInfo postInfo;

	/**
	 * Constructs a new Post with a random ID.
	 *
	 * @param data          the data to be encapsulated in the Post
	 * @param type          the Type of the data of the Post
	 * @param poster        the Poster of the Post
	 * @param fileExtension the file extension associated with the data of the Post.
	 *                      Must be {@code null} if {@code type == DataType.TEXT}.
	 * @param topic         the Topic of the Post
	 */
	public Post(byte[] data, DataType type, Profile poster, String fileExtension, Topic topic) {
		this(data, type, poster, fileExtension, topic, ThreadLocalRandom.current().nextLong());
	}

	/**
	 * Constructs a new Post with a set ID.
	 *
	 * @param data          the data to be encapsulated in the Post
	 * @param type          the Type of the data of the Post
	 * @param poster        the Poster of the Post
	 * @param fileExtension the file extension associated with the data of the Post.
	 *                      Must be {@code null} if {@code type == DataType.TEXT}.
	 * @param topic         the Topic of the Post
	 * @param postID        the id of the Post
	 */
	private Post(byte[] data, DataType type, Profile poster, String fileExtension, Topic topic,
	        long postID) {
		if ((type == DataType.TEXT) && (fileExtension != null))
			throw new IllegalArgumentException("DataType TEXT requires 'null' file extension");

		this.data = data;
		this.postInfo = new PostInfo(type, poster, fileExtension, topic, postID);
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
}
