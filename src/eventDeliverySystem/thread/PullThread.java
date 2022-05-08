package eventDeliverySystem.thread;

import java.io.IOException;
import java.io.ObjectInputStream;

import eventDeliverySystem.LG;
import eventDeliverySystem.datastructures.AbstractTopic;
import eventDeliverySystem.datastructures.Packet;
import eventDeliverySystem.datastructures.PostInfo;

/**
 * A Thread that reads some Posts from a stream and then posts them to a Topic.
 *
 * @author Alex Mandelias
 */
public class PullThread extends Thread {

	private final ObjectInputStream ois;
	private final AbstractTopic     topic;

	private boolean success, start, end;

	/**
	 * Constructs the Thread that, when run, will read some Posts from a stream and
	 * post them to a Topic.
	 *
	 * @param stream the input stream from which to read the Posts
	 * @param topic  the Topic in which the new Posts will be added
	 */
	public PullThread(ObjectInputStream stream, AbstractTopic topic) {
		super("PullThread-" + topic.getName());
		ois = stream;
		this.topic = topic;
		success = start = end = false;
	}

	@Override
	public void run() {
		LG.sout("%s#run()", getName());
		LG.in();
		start = true;

		try {

			final int postCount = ois.readInt();
			LG.sout("postCount=%d", postCount);
			LG.in();

			for (int i = 0; i < postCount; i++) {

				final PostInfo postInfo;
				try {
					postInfo = (PostInfo) ois.readObject();
				} catch (final ClassNotFoundException e) {
					e.printStackTrace();
					return;
				}

				LG.sout("postInfo=%s", postInfo);
				topic.post(postInfo);

				Packet packet;
				do {
					try {
						packet = (Packet) ois.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}
					topic.post(packet);
				} while (!packet.isFinal());
			}

			LG.out();

			success = true;

		} catch (final IOException e) {
			e.printStackTrace();
			success = false;
		}

		LG.sout("success=%s", success);

		end = true;
		LG.out();
		LG.sout("/%s#run()", getName());
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
