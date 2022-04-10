package eventDeliverySystem;

import java.util.concurrent.ThreadLocalRandom;

/**
 * An object holding information about a posts's data, its
 * poster and its type.
 *
 */
class RawData {
	
	private final Profile poster;
	private final byte[] data;
	private final DataType type;
	private final long postID;
	
	public RawData(byte[] data, DataType type, Profile poster) {
		this.data = data;
		this.type = type;
		this.poster = poster;
		this.postID = ThreadLocalRandom.current().nextLong();
	}
	
	public byte[] getData() {
		return data.clone();
	}
	
	public DataType getType() {
		return type;
	}
	
	public Profile getPosterName() {
		return poster;
	}

	public Profile getPoster() {
		return poster;
	}

	public long getPostID() {
		return postID;
	}
}

