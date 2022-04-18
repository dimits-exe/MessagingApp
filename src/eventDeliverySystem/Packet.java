package eventDeliverySystem;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

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
	private static final long serialVersionUID = 1;

	/**
	 * Turn a file or text to a series of packets.
	 *
	 * @param data          the data of the file
	 * @param fileExtension the extension of the file
	 *
	 * @return an array of packet objects holding the file's data
	 */
	public static Packet[] dataToPackets(RawData data, String fileExtension) {
		final byte[] src = data.getData();

		final int      packetCount = (int) Math.ceil(src.length / (double) Packet.PACKET_SIZE);
		final Packet[] packets     = new Packet[packetCount];
		final long     id          = Packet.getRandomID(); // same for packets of the same RawData

		for (int i = 0; i < packetCount; i++) {
			final boolean  isFinal = i == (packetCount - 1);
			final byte[]   payload = new byte[Packet.PACKET_SIZE];
			final DataType type    = data.getType();
			final Profile  poster  = data.getPoster();

			if (!isFinal)
				System.arraycopy(src, i * Packet.PACKET_SIZE, payload, 0, Packet.PACKET_SIZE);
			else
				System.arraycopy(src, i * Packet.PACKET_SIZE, payload, 0,
				        src.length - (i * Packet.PACKET_SIZE));

			final Packet packet = new Packet(id, isFinal, payload, type, fileExtension, poster);
			packets[i] = packet;
		}

		return packets;
	}

	/**
	 * Turn a string of text to a series of packets.
	 *
	 * @param text   the text to be packaged
	 * @param poster the profile of the user that sent the text
	 *
	 * @return an array of packet objects holding the text
	 */
	public static Packet[] textToPackets(String text, Profile poster) {
		final RawData rawData = new RawData(text.getBytes(), DataType.TEXT, poster, null);
		return Packet.dataToPackets(rawData, null);
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
	public static RawData packetsToData(Packet[] packets) {
		if (packets.length == 0)
			throw new IllegalArgumentException("Tried to create a RawData object with no data");

		final byte[] data = new byte[Stream.of(packets).mapToInt(p -> p.payload.length).sum()];
		int          ptr  = 0;

		final long idOfFirst = packets[0].id;

		for (int i = 0, count = packets.length; i < count; i++) {
			final Packet curr = packets[i];

			if (curr.id != idOfFirst)
				throw new IllegalStateException(String.format(
				        "Tried to packet with id %d with packet with id %d", idOfFirst, curr.id));

			final byte[] payload = curr.payload;
			final int    length  = payload.length;
			System.arraycopy(payload, 0, data, ptr, length);
			ptr += length;
		}

		return new RawData(data, packets[0].type, packets[0].poster,
		        packets[0].fileExtension);
	}

	private static long getRandomID() {
		return ThreadLocalRandom.current().nextLong();
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
}
