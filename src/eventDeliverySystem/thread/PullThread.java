package eventDeliverySystem.thread;

import java.io.IOException;
import java.io.ObjectInputStream;

import eventDeliverySystem.datastructures.AbstractTopic;
import eventDeliverySystem.datastructures.Packet;
import eventDeliverySystem.datastructures.PostInfo;
import eventDeliverySystem.util.LG;

/**
 * A Thread that reads some Posts from a stream and then posts them to a Topic.
 *
 * @author Alex Mandelias
 */
public class PullThread extends Thread {

	private final ObjectInputStream ois;
	private final AbstractTopic     topic;

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
	}

	@Override
	public void run() {
		LG.sout("%s#run()", getName());
		LG.in();

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
					} catch (final ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}
					topic.post(packet);
				} while (!packet.isFinal());
			}

			LG.out();

		} catch (final IOException e) {
			e.printStackTrace();
		}

		LG.out();
		LG.sout("/%s#run()", getName());
	}
}
