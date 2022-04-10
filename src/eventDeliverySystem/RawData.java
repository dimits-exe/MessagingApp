package eventDeliverySystem;

/**
 * An object holding information about a posts's data, its
 * poster and its type.
 *
 */
class RawData {
	
	private final String posterName;
	private final byte[] data;
	private final DataType type;
	
	public RawData(byte[] data, DataType type, String posterName) {
		this.data = data;
		this.type = type;
		this.posterName = posterName;
	}
	
	public byte[] getData() {
		return data.clone();
	}
	
	public DataType getType() {
		return type;
	}
	
	public String getPosterName() {
		return posterName;
	}
}
