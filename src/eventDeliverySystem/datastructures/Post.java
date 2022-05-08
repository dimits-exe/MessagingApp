package eventDeliverySystem.datastructures;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * Encapsulates a Post, its data and its associated PostInfo object.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 *
 * @see PostInfo
 */
public class Post {

	/**
	 * Extracts the data of some Packets into a single Post.
	 *
	 * @param packets  an array of packets that stores the data of a Post
	 * @param postInfo the PostInfo object associated with that Post
	 *
	 * @return the packets' contents as a Post
	 *
	 * @throws IllegalArgumentException if the packets array has length 0
	 * @throws IllegalStateException    if Packets with different IDs are found in
	 *                                  the packets array
	 */
	public static Post fromPackets(Packet[] packets, PostInfo postInfo) {
		if (packets.length == 0)
			throw new IllegalArgumentException("Tried to create a Post object with no data");

		final int    byteCount = Stream.of(packets).mapToInt(p -> p.getPayload().length).sum();
		final byte[] data      = new byte[byteCount];

		final long idOfFirst = packets[0].getPostId();

		int dataPointer = 0;
		for (int i = 0, packetCount = packets.length; i < packetCount; i++) {
			final Packet curr = packets[i];

			if (curr.getPostId() != idOfFirst)
				throw new IllegalStateException(String.format(
				        "Tried to combine packet with ID %d with packet with ID %d", idOfFirst,
				        curr.getPostId()));

			final byte[] payload = curr.getPayload();
			final int    length  = payload.length;
			System.arraycopy(payload, 0, data, dataPointer, length);
			dataPointer += length;
		}

		return new Post(data, postInfo);
	}

	/**
	 * Constructs a Post with from a File.
	 *
	 * @param file     the File whose data will be encapsulated in a Post
	 * @param posterId the ID of the poster of the File
	 *
	 * @return the Post that encapsulates the contents of the File
	 *
	 * @throws FileNotFoundException if the File could not be found
	 * @throws IOException           if an I/O Error occurs
	 */
	public static Post fromFile(File file, long posterId)
	        throws FileNotFoundException, IOException {

		byte[] data;
		try (FileInputStream fis = new FileInputStream(file)) {
			data = fis.readAllBytes();
		}

		final String fileName      = file.getName();
		final String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);
		return new Post(data, posterId, fileExtension);
	}

	/**
	 * Constructs a Post from a String of text, a plain-text message.
	 *
	 * @param text     the message
	 * @param posterId the ID of the poster of the plain-message
	 *
	 * @return the Post that encapsulates the plain-text message
	 */
	public static Post fromText(String text, long posterId) {
		return new Post(text.getBytes(), posterId, "~txt");
	}

	private final byte[]   data;
	private final PostInfo postInfo;

	/**
	 * Constructs a new Post with the specified info.
	 *
	 * @param data     the contents of this post
	 * @param postInfo the PostInfo object associated with this Post
	 */
	public Post(byte[] data, PostInfo postInfo) {
		this.data = data;
		this.postInfo = postInfo;
	}

	/**
	 * Constructs a new Post with a random ID.
	 *
	 * @param data          the data to be encapsulated in this Post
	 * @param posterId      the ID of the poster of this Post
	 * @param fileExtension the file extension associated with the data of this
	 *                      Post. Plain-text messages have a file extension of
	 *                      '{@code ~txt}'
	 */
	private Post(byte[] data, long posterId, String fileExtension) {
		this(data, posterId, fileExtension, ThreadLocalRandom.current().nextLong());
	}

	/**
	 * Constructs a new Post with a set ID.
	 *
	 * @param data          the data to be encapsulated in this Post
	 * @param posterId      the ID of the poster of this Post
	 * @param fileExtension the file extension associated with the data of this
	 *                      Post. Plain-text messages have a file extension of
	 *                      '{@code ~txt}'
	 * @param postID        the ID of this Post
	 */
	private Post(byte[] data, long posterId, String fileExtension, long postID) {
		this(data, new PostInfo(posterId, fileExtension, postID));
	}

	/**
	 * Returns a clone of this Post's data.
	 *
	 * @return a clone of the data
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
		return String.format("%s: %d bytes", postInfo, data.length);
	}
}
