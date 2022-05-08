package eventDeliverySystem.datastructures;

import java.io.Serializable;

/**
 * Provides utility methods to facilitate the transmission of Posts by breaking
 * them into Packets and then reassembling them.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
public class Packet implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final int PACKET_SIZE = (int) (512 * Math.pow(2, 10));

	/**
	 * Break a Post into an array of Packets.
	 *
	 * @param post the Post
	 *
	 * @return an array of Packets which collectively stores the original Post
	 */
	public static Packet[] fromPost(Post post) {
		final byte[]   src      = post.getData();
		final PostInfo postInfo = post.getPostInfo();
		final long     id       = postInfo.getId();

		final int      packetCount = (int) Math.ceil(src.length / (double) Packet.PACKET_SIZE);
		final Packet[] packets     = new Packet[packetCount];

		int srcPointer = 0;
		for (int i = 0; i < packetCount; i++) {
			final boolean isFinal = i == (packetCount - 1);

			final int    length  = Math.min(Packet.PACKET_SIZE, src.length - srcPointer);
			final byte[] payload = new byte[length];

			System.arraycopy(src, srcPointer, payload, 0, length);
			srcPointer += length;

			packets[i] = new Packet(isFinal, payload, id);
		}

		return packets;
	}

	private final boolean isFinal;
	private final byte[]  payload;
	private final long    postId;

	private Packet(boolean isFinal, byte[] payload, long postId) {
		this.isFinal = isFinal;
		this.payload = payload;
		this.postId = postId;
	}

	/**
	 * Returns whether there are more remaining packets for the associated Post.
	 *
	 * @return {@code true} if this is the last packet for the Post, {@code false}
	 *         otherwise
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
	 * Returns the id of the Post this Packet is a part of.
	 *
	 * @return the id of its associated Post
	 */
	public long getPostId() {
		return postId;
	}
}
