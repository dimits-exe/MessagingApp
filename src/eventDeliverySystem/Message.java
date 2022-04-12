package eventDeliverySystem;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * An object to be sent between remote system components to
 * describe any change in the state of that system (for example process crashes).
 *
 */
class Message implements Serializable {
	private static final long serialVersionUID = 8083153797545703062L;
	
	private final MessageType type;
	private final InetAddress address;
	
	
	/**
	 * Construct a message with the given {@link MessageType} type and address.
	 * @param type the type of the message
	 * @param address the IP address the message is referring to
	 */
	public Message(MessageType type, InetAddress address) {
		this.type = type;
		this.address = address;
	}

	public MessageType getType() {
		return type;
	}

	public InetAddress getAddress() {
		return address;
	}
	
	enum MessageType{
		REDIRECT, ERROR
	}
}
