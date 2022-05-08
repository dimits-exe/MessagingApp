package eventDeliverySystem;

/**
 * TODO
 *
 *
 * @author Alex Mandelias
 */
interface Subscriber {

	void notify(PostInfo postInfo, String topicName);

	void notify(Packet packet, String topicName);
}
