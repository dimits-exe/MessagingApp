package eventDeliverySystem.datastructures;

import java.io.Serializable;

/**
 * A wrapper holding an object and specifying its type using an enum used for
 * Internet transportation. Used to facilitate uniform communication between all
 * remote components of the system.
 *
 * @author Dimitris Tsirmpas
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 1L;

	private final MessageType type;
	private final Object      value;

	/**
	 * Constructs a Message with the given {@link MessageType type} and value.
	 *
	 * @param type  the type of the message
	 * @param value the value of the message
	 */
	public Message(MessageType type, Object value) {
		this.type = type;
		this.value = value;
	}

	/**
	 * Returns this Message's type.
	 *
	 * @return the type
	 */
	public MessageType getType() {
		return type;
	}

	/**
	 * Returns this Message's value.
	 *
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Specifies the purpose of a Message, thereby indirectly indicating the type of
	 * its value.
	 *
	 * @author Alex Mandelias
	 * @author Dimitris Tsirmpas
	 */
	public enum MessageType {

		/**
		 * Indicates the start of a data send message. The value is a String describing the Topic's name.
		 */
		DATA_PACKET_SEND,

		/** Requests the actual broker for a Topic. The value is is a String describing the Topic's name. */
		PUBLISHER_DISCOVERY_REQUEST,

		/**
		 * Request to initialise consumer connection to broker. The value is a
		 * TopicToken.
		 */
		INITIALISE_CONSUMER,

		/**
		 * Request the (not-default) broker to create a new Topic.
		 * The value is is a String describing the new Topic's name.
		 */
		CREATE_TOPIC,

		/**
		 * Inform the broker that another broker has connected.
		 * The value is the ConnectionInfo of the newly created Broker.
		 *
		 * Warning: in case of a multicast the accepting socket and the serialized
		 * ConnectionInfo object may NOT represent the same broker.
		 */
		BROKER_CONNECTION
	}
}
