package eventDeliverySystem;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * TODO
 *
 *
 * @author Alex Mandelias
 */
abstract class AbstractTopic {

	private final String name;

	/**
	 * TODO
	 */
	public AbstractTopic(String name) {
		this.name = name;
	}

	/**
	 * Returns this Topic's name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the ID of the most recent Packet in this Topic.
	 *
	 * @return the most recent Packet's ID or {@link Topic#FETCH_ALL_POSTS}
	 *         if there are no Packets in this Topic
	 */
	// public abstract long getLastId();

	abstract public void post(PostInfo postInfo);

	abstract public void post(Packet packet);

	/**
	 * Returns the hash that a Topic with a given name would have. Since a Topic's
	 * hash is determined solely by its name, this method returns the same result as
	 * Topic#hashCode(), when given the name of the Topic, and can be used when an
	 * instance of Topic is not available, but its name is known.
	 *
	 * @param topicName the name of the Topic for which to compute the hash
	 *
	 * @return a hash code value for this Topic
	 */
	public static int hashForTopic(String topicName) {
		try {
			MessageDigest a = MessageDigest.getInstance("md5");
			byte[]        b = a.digest(topicName.getBytes());

			// big brain stuff
			final int FOUR = 4;
			int       c    = FOUR;
			int       d    = b.length / c;
			byte[]    e    = new byte[c];
			for (int f = 0; f < e.length; f++)
				for (int g = 0; g < d; g++)
					e[f] ^= (b[(d * f) + g]);

			BigInteger h = new BigInteger(e);
			return h.intValueExact();

		} catch (NoSuchAlgorithmException | ArithmeticException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode() {
		return AbstractTopic.hashForTopic(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof AbstractTopic))
			return false;
		AbstractTopic other = (AbstractTopic) obj;
		return Objects.equals(name, other.name); // same name == same Topic, can't have duplicate names
	}
}
