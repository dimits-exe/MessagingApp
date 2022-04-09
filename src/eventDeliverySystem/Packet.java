package eventDeliverySystem;

import java.io.Serializable;

/**
 * A class holding binary data for to-be-transmitted files and text.
 */
class Packet implements Serializable {
	private static final int PACKET_SIZE = 1000000; // 1 MB Chunk size
	private static final long serialVersionUID = -3948707845170731230L;
	
	/**
	 * Turn a file or text to a series of packets.
	 * @param data the binary data of the file
	 * @param type the type of the file
	 * @param fileExtension the extension of the file
	 * @return an array of packet objects holding the file's data
	 */
	public static Packet[] bytesToPackets(byte[] data, DataType type, String fileExtension) {
		Packet[] packets = new Packet[(int) Math.ceil(data.length/PACKET_SIZE)];
		//fill the array with packets lol
		return packets;
	}
	
	/**
	 * Turn a string of text to a series of packets.
	 * @param text the text to be packaged
	 * @return an array of packet objects holding the text
	 */
	public static Packet[] textToPackets(String text) {
		return Packet.bytesToPackets(null, DataType.TEXT, null);
	}
	
	/**
	 * Combine the contents of many packets to a single byte array.
	 * @param packets an array with packets holding the entire contents of the file
	 * @return the packets' contents
	 * @throws IllegalStateException if there is any error with the contents of the packets
	 */
	public static byte[] packetsToBytes(Packet[] packets) throws IllegalStateException {
		return null;
	}
	
	private final int id;
	private final boolean isFinal;
	private final byte[] payload;
	private final String fileExtension;
	private final DataType type;
	private final int errorDetectionCode;
	//private final int order; should be unused because of TCP guaranteeing correct order
	
	private Packet(int id, boolean isFinal, byte[] payload, int errorDetectionCode ,DataType type, String fileExtension) {
		this.id = id;
		this.isFinal = isFinal;
		this.payload = payload;
		this.errorDetectionCode = errorDetectionCode;
		this.type = type;
		this.fileExtension = fileExtension;
	}
	
	/**
	 * Get a unique identification number of the file chunk contained in the packet.
	 * @return the id of the file chunk
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Whether or not there are more packets for this file on the way.
	 * @return true if this is the last packet for the file, false otherwise
	 */
	public boolean isFinal() {
		return isFinal;
	}
	
	/**
	 * Returns a copy of the contents of the packet.
	 * @return a copy of the packet's data
	 */
	public byte[] getPayload() {
		return payload.clone();
	}
	
	/**
	 * Get the file extension of the file.
	 * @return the file extension
	 * @throws IllegalArgumentException if the contents of the packet are of the DataType "TEXT"
	 */
	public String getFileExtension() throws IllegalArgumentException {
		if(type == DataType.TEXT)
			throw new IllegalArgumentException("The data of the packet is a string object, not a file.");
		return fileExtension;
	}

	/**
	 * Get the {@link DataType} type of the packet.
	 * @return the type of the data
	 */
	public DataType getType() {
		return type;
	}

	/**
	 * Get a hashcode of the original file to be compared after the file is reconstructed.
	 * @return the hashcode of the original file
	 */
	public int getErrorDetectionCode() {
		return errorDetectionCode;
	}
	
	
}
