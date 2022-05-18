package eventDeliverySystem.server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import eventDeliverySystem.datastructures.AbstractTopic;
import eventDeliverySystem.datastructures.Packet;
import eventDeliverySystem.datastructures.PostInfo;
import eventDeliverySystem.util.LG;
import eventDeliverySystem.util.Subscriber;

/**
 * A thread that receives packets for a certain Topic and streams them to a
 * Consumer.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
class BrokerPushThread extends Thread implements Subscriber {

	private long                          currentPostId;
	private final Deque<PostInfo>         postInfos;
	private final Map<Long, List<Packet>> buffers;

	private final Queue<Object>      queue;
	private final ObjectOutputStream oos;

	/**
	 * Constructs the Thread that, when run, will write some Posts to a stream. This
	 * Thread is subscribed to a Topic and is notified each time there is new data
	 * in the Topic.
	 *
	 * @param topic  the Topic to subscribe to
	 * @param stream the output stream to which to write the data
	 */
	public BrokerPushThread(AbstractTopic topic, ObjectOutputStream stream) {
		super("BrokerPushThread-" + topic.getName());
		topic.subscribe(this);
		queue = new LinkedList<>();
		currentPostId = -1;
		postInfos = new LinkedList<>();
		buffers = new HashMap<>();
		oos = stream;
	}

	@Override
	public void run() {
		boolean isEmpty;
		while (true) {
			synchronized (queue) {
				isEmpty = queue.isEmpty();
			}

			while (isEmpty) {
				LG.sout("--- queue is empty ---");
				try {
					synchronized (this) {
						this.wait();
					}
				} catch (final InterruptedException e) {}

				LG.sout("--- queue notified ---");

				synchronized (queue) {
					isEmpty = queue.isEmpty();
				}
			}

			LG.sout("--- queue is no longer empty ---");
			do {
				LG.sout("--- there is more stuff to send ---");
				try {
					synchronized (queue) {
						oos.writeObject(queue.remove());
					}
				} catch (final IOException e) {
					e.printStackTrace();
				}

				synchronized (queue) {
					isEmpty = queue.isEmpty();
				}
			} while (isEmpty);
		}
	}

	@Override
	synchronized public void notify(PostInfo postInfo, String topicName) {
		LG.sout("BrokerPushThread#notify(%s)", postInfo);

		// if no post is being streamed
		if (currentPostId == -1) {
			// set post as current being streamed
			currentPostId = postInfo.getId();
			// start streaming post
			queue.add(postInfo);

		} else {
			// add this post to buffer
			postInfos.addLast(postInfo);
			buffers.put(postInfo.getId(), new LinkedList<>());
		}

		this.notify();
	}

	@Override
	synchronized public void notify(Packet packet, String topicName) {
		LG.sout("BrokerPushThread#notify(%s)", packet);

		// if no post is being streamed
		if (currentPostId == -1)
			throw new RuntimeException("currentPostId can't be -1 at this pont");

		// if packet belongs to post being streamed
		if (packet.getPostId() == currentPostId) {
			// stream packet
			queue.add(packet);

			// if current post is fully streamed
			if (packet.isFinal()) {

				// start streaming next post
				boolean finalReached;
				do {
					finalReached = false;

					// if no posts left in buffer, mark current as none
					// wait next post info
					if (postInfos.isEmpty()) {
						currentPostId = -1;
						break;
					}

					// take next Post
					final PostInfo curr = postInfos.removeFirst();

					// set as current
					currentPostId = curr.getId();

					// start streaming post
					queue.add(curr);

					// stream all packets in buffer
					final List<Packet> buffer = buffers.get(currentPostId);
					for (final Packet packetInBuffer : buffer) {
						if (finalReached)
							throw new RuntimeException(
							        "this should never happend tomara deikse eleos");

						// stream packet
						queue.add(packetInBuffer);

						// mark if this post has been fully streamed
						finalReached |= packetInBuffer.isFinal();
					}

					if (finalReached) {
						buffers.remove(currentPostId);
					}

					// keep streaming the next post in buffer if the previous has been fully streamed
				} while (finalReached);
			}
		} else {
			// add packet to buffer because it's not being streamed
			buffers.get(packet.getPostId()).add(packet);
		}

		this.notify();
	}
}
