package eventDeliverySystem;

import java.util.concurrent.ThreadLocalRandom;

/**
 * An object holding information about a posts's data, its poster and its type.
 *
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
	 * Constructs a new RawData object.
	 *
	 * @param data          the data to be encapsulated in the RawData object
	 * @param type          the Type of the data
	 * @param poster        the Poster of the data
	 * @param fileExtension the file extension associated with the data. Must be
	 *                      {@code null} if {@code type == DataType.TEXT}.
	 */
	public RawData(byte[] data, DataType type, Profile poster, String fileExtension) {
		if ((type == DataType.TEXT) && (fileExtension != null))
			throw new IllegalArgumentException("DataType TEXT requires 'null' file extension");

		this.data = data;
		this.type = type;
		this.poster = poster;
		this.fileExtension = fileExtension;
		this.postID = ThreadLocalRandom.current().nextLong();
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
