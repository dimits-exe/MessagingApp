package eventDeliverySystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * A Thread that reads some Posts from a stream and then posts them to a Topic.
 *
 * @author Alex Mandelias
 */
class PullThread extends Thread {

	private final ObjectInputStream ois;
	private final Topic             topic;

	private boolean success, start, end;

	/**
	 * Constructs the Thread that, when run, will read some Posts from a stream and
	 * post them to a Topic.
	 *
	 * @param stream the input stream from which to read the Posts
	 * @param topic  the Topic in which the new Posts will be added
	 */
	public PullThread(ObjectInputStream stream, Topic topic) {
		ois = stream;
		this.topic = topic;
		success = start = end = false;
	}

	@Override
	public void run() {
		start = true;

		try /* (ois) */ {

			final int postCount = ois.readInt();

			for (int i = 0; i < postCount; i++) {

				final PostInfo postInfo;
				try {
					postInfo = (PostInfo) ois.readObject();
				} catch (final ClassNotFoundException e) {
					e.printStackTrace();
					return;
				}

				final List<Packet> packets = new LinkedList<>();
				Packet             packet;
				do {
					try {
						packet = (Packet) ois.readObject();
					} catch (final ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}
					packets.add(packet);
				} while (!packet.isFinal());

				final Packet[] packetArray = new Packet[packets.size()];
				packets.toArray(packetArray);
				final Post newPost = Post.fromPackets(packetArray, postInfo);
				topic.post(newPost);
			}

			success = true;

		} catch (final IOException e) {
			System.err.printf("IOException in PullThread#run()%n");
			e.printStackTrace();
			success = false;
		}

		end = true;
	}

	/**
	 * Returns whether this Thread has executed its job successfully. This method
	 * shall be called after this Thread has executed its {@code run} method once.
	 *
	 * @return {@code true} if it has, {@code false} otherwise
	 *
	 * @throws IllegalStateException if this Thread has not completed its execution
	 *                               before this method is called
	 */
	public boolean success() throws IllegalStateException {
		if (!start)
			throw new IllegalStateException(
			        "Can't call 'success()' before starting this Thread");
		if (!end)
			throw new IllegalStateException(
			        "This Thread must finish execution before calling 'success()'");

		return success;
	}
}
