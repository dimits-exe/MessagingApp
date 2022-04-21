package eventDeliverySystem;

import java.io.Serializable;

import eventDeliverySystem.Post.DataType;

/**
 * Provides utility methods to facilitate the transmission of RawData objects by
 * breaking them into packets and then reassembling them.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
class Packet implements Serializable {

	private static final int  PACKET_SIZE      = (int) (512 * Math.pow(2, 10));
	private static final long serialVersionUID = 1L;

	/**
	 * Turn a file to a series of packets.
	 *
	 * @param data          the data of the file
	 * @param fileExtension the extension of the file
	 *
	 * @return an array of packet objects holding the file's data
	 */
	public static Packet[] fromPost(Post data) {
		final byte[] src = data.getData();
		int          ptr = 0;

		final int      packetCount = (int) Math.ceil(src.length / (double) Packet.PACKET_SIZE);
		final Packet[] packets     = new Packet[packetCount];
		final PostInfo postInfo    = data.getPostInfo();

		for (int i = 0; i < packetCount; i++) {
			final boolean  isFinal = i == (packetCount - 1);
			final byte[]   payload = new byte[Packet.PACKET_SIZE];

			final int length = Math.min(Packet.PACKET_SIZE, src.length - ptr);
			System.arraycopy(src, (ptr += length) - length, payload, 0, length);

			packets[i] = new Packet(isFinal, payload, postInfo.getId());
		}

		return packets;
	}

	/**
	 * Turn a string of text to a series of packets.
	 *
	 * @param text   the text
	 * @param poster the profile of the user that sent the text
	 * @param topic  the
	 *
	 * @return an array of packet objects holding the text
	 */
	public static Packet[] fromText(String text, Profile poster, Topic topic) {
		final Post rawData = new Post(text.getBytes(), DataType.TEXT, poster, null, topic);
		return Packet.fromPost(rawData);
	}

	private final boolean  isFinal;
	private final byte[]   payload;
	private final long postId;

	private Packet(boolean isFinal, byte[] payload, long postId) {
		this.isFinal = isFinal;
		this.payload = payload;
		this.postId = postId;
	}

	/**
	 * Returns whether there are more remaining packets for this RawData object.
	 *
	 * @return {@code true} if this is the last packet for the RawData object,
	 *         {@code false} otherwise
	 */
	public boolean isFinal() {
		return isFinal;
	}

	/**
	 * Returns this Packet's payload.
	 *
	 * @return the payload
	 */
	public byte[] getPayload() {
		return payload;
	}

	/**
	 * Returns this Packet's id.
	 *
	 * @return the id that corresponds to the post being sent
	 */
	public long getPostId() {
		return postId;
	}
}
