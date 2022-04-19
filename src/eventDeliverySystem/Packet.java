package eventDeliverySystem;

import java.io.Serializable;

import eventDeliverySystem.RawData.DataType;

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
	public static Packet[] fromRawData(RawData data, String fileExtension) {
		final byte[] src = data.getData();
		int          ptr = 0;

		final int      packetCount = (int) Math.ceil(src.length / (double) Packet.PACKET_SIZE);
		final Packet[] packets     = new Packet[packetCount];
		final long     id          = data.getPostID(); // same for packets of the same RawData

		for (int i = 0; i < packetCount; i++) {
			final boolean  isFinal = i == (packetCount - 1);
			final byte[]   payload = new byte[Packet.PACKET_SIZE];
			final DataType type    = data.getType();
			final Profile  poster  = data.getPoster();
			final int      length  = Math.min(Packet.PACKET_SIZE, src.length - ptr);

			System.arraycopy(src, (ptr += length) - length, payload, 0, length);

			packets[i] = new Packet(id, isFinal, payload, type, fileExtension, poster);
		}

		return packets;
	}

	/**
	 * Turn a string of text to a series of packets.
	 *
	 * @param text   the text
	 * @param poster the profile of the user that sent the text
	 *
	 * @return an array of packet objects holding the text
	 */
	public static Packet[] fromText(String text, Profile poster) {
		final RawData rawData = new RawData(text.getBytes(), DataType.TEXT, poster, null);
		return Packet.fromRawData(rawData, null);
	}

	private final long     id;
	private final boolean  isFinal;
	private final byte[]   payload;
	private final String   fileExtension;
	private final Profile  poster;
	private final DataType type;

	private Packet(long id, boolean isFinal, byte[] payload, DataType type, String fileExtension,
	        Profile poster) {
		this.id = id;
		this.isFinal = isFinal;
		this.payload = payload;
		this.poster = poster;
		this.type = type;
		this.fileExtension = fileExtension;
	}

	/**
	 * Returns this Packet's ID.
	 *
	 * @return the ID
	 */
	public long getId() {
		return id;
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
	 * Returns this Packet's fileExtension.
	 *
	 * @return the fileExtension
	 */
	public String getFileExtension() {
		return fileExtension;
	}

	/**
	 * Returns this Packet's poster.
	 *
	 * @return the poster
	 */
	public Profile getPoster() {
		return poster;
	}

	/**
	 * Returns this Packet's type.
	 *
	 * @return the type
	 */
	public DataType getType() {
		return type;
	}
}
