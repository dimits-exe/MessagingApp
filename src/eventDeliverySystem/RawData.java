package eventDeliverySystem;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * An object holding information about a posts's data, its poster and its type.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
class RawData {

	public enum DataType {
		TEXT, VIDEO, IMAGE
	}

	private final byte[]   data;
	private final DataType type;
	private final Profile  poster;
	private final String   fileExtension;
	private final long     postID;

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
	public static RawData fromPackets(Packet[] packets) {
		if (packets.length == 0)
			throw new IllegalArgumentException("Tried to create a RawData object with no data");

		final byte[] data = new byte[Stream.of(packets).mapToInt(p -> p.getPayload().length).sum()];
		int          ptr  = 0;

		final long idOfFirst = packets[0].getId();

		for (int i = 0, count = packets.length; i < count; i++) {
			final Packet curr = packets[i];

			if (curr.getId() != idOfFirst)
				throw new IllegalStateException(String.format(
				        "Tried to combine packet with id %d with packet with id %d", idOfFirst,
				        curr.getId()));

			final byte[] payload = curr.getPayload();
			final int    length  = payload.length;
			System.arraycopy(payload, 0, data, (ptr += length) - length, length);
		}

		final Packet p = packets[0];
		return new RawData(data, p.getType(), p.getPoster(), p.getFileExtension(), p.getId());
	}

	/**
	 * Constructs a new RawData object with a random ID.
	 *
	 * @param data          the data to be encapsulated in the RawData object
	 * @param type          the Type of the data
	 * @param poster        the Poster of the data
	 * @param fileExtension the file extension associated with the data. Must be
	 *                      {@code null} if {@code type == DataType.TEXT}.
	 */
	public RawData(byte[] data, DataType type, Profile poster, String fileExtension) {
		this(data, type, poster, fileExtension, ThreadLocalRandom.current().nextLong());
	}

	/**
	 * Constructs a new RawData object with a set ID.
	 *
	 * @param data          the data to be encapsulated in this RawData object
	 * @param type          the Type of the data
	 * @param poster        the Poster of the data
	 * @param fileExtension the file extension associated with the data. Must be
	 *                      {@code null} if {@code type == DataType.TEXT}.
	 * @param postID        the id of this RawData object
	 */
	private RawData(byte[] data, DataType type, Profile poster, String fileExtension, long postID) {
		if ((type == DataType.TEXT) && (fileExtension != null))
			throw new IllegalArgumentException("DataType TEXT requires 'null' file extension");

		this.data = data;
		this.type = type;
		this.poster = poster;
		this.fileExtension = fileExtension;
		this.postID = postID;
	}

	/**
	 * Returns the poster.
	 *
	 * @return the poster
	 */
	public Profile getPoster() {
		return poster;
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
	 * Returns the type.
	 *
	 * @return the type
	 */
	public DataType getType() {
		return type;
	}

	/**
	 * Returns the fileExtension.
	 *
	 * @return the fileExtension
	 */
	public String getFileExtension() {
		return fileExtension;
	}

	/**
	 * Returns the postID.
	 *
	 * @return the postID
	 */
	public long getPostID() {
		return postID;
	}
}
