package eventDeliverySystem;

import java.io.Serializable;

/**
 * A wrapper holding an object and specifying its type using an enum used for Internet transportation.
 * Used to facilitate uniform communication between all remote components of the system.
 */
class Message implements Serializable {
	private static final long serialVersionUID = 8083163797545703062L;
	
	private final MessageType type;
	private final Object value;
	
	
	/**
	 * Construct a message with the given {@link MessageType} type and address.
	 * @param type the type of the message
	 * @param address the IP address the message is referring to
	 */
	public Message(MessageType type, Object value) {
		this.type = type;
		this.value = value;
	}

	public MessageType getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}
	
	/**
	 * Specifies the purpose and type of payload of the message.
	 */
	enum MessageType{
		/**
		 * Change the connecting IP to a new one.
		 * The value is a ConnectionInfo object.
		 */
		REDIRECT, 
		/**
		 * An error has occurred while trying to reach a certain process.
		 * The value is a ConnectionInfo object.
		 */
		ERROR,
		/**
		 * A simple data message.
		 * The value is a Packet object.
		 */
		DATA,
		/**
		 * A new IP has been connected to the system.
		 * The value is a ConnectionInfo object.
		 */
		DISCOVER
	}
}
