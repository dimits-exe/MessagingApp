package eventDeliverySystem.thread;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import eventDeliverySystem.LG;
import eventDeliverySystem.Subscriber;
import eventDeliverySystem.datastructures.AbstractTopic;
import eventDeliverySystem.datastructures.Packet;
import eventDeliverySystem.datastructures.PostInfo;

/**
 * A thread that receives packets for a certain Topic
 * and streams them to a Consumer.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 *
 */
public class BrokerPushThread extends Thread implements Subscriber {

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
		queue = new ConcurrentLinkedDeque<>();
		currentPostId = -1;
		postInfos = new ConcurrentLinkedDeque<>();
		buffers = Collections.synchronizedMap(new HashMap<>());
		oos = stream;
	}

	@Override
	public void run() {
		while (true) {
			while (queue.isEmpty()) {
				LG.sout("--- queue is empty ---");
				try {
					synchronized (this) {
						this.wait();
					}
				} catch (InterruptedException e) {}
			}

			LG.sout("--- queue is no longer empty ---");
			do {
				try {
					oos.writeObject(queue.remove());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} while (!queue.isEmpty());
		}
	}

	@Override
	public void notify(PostInfo postInfo, String topicName) {
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
			buffers.put(postInfo.getId(), Collections.synchronizedList(new LinkedList<>()));
		}

		synchronized (this) {
			this.notify();
		}
	}

	@Override
	public void notify(Packet packet, String topicName) {
		LG.sout("BrokerPushThread#notify(%s)", packet);

		// if no post is being streamed
		if (currentPostId == -1) {
			throw new RuntimeException("currentPostId can't be -1 at this pont");
		}

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
					PostInfo curr = postInfos.removeFirst();
					// set as current
					currentPostId = curr.getId();
					// start streaming post
					queue.add(curr);

					// stream all packets in buffer
					List<Packet> buffer = buffers.get(currentPostId);
					for (Packet packetInBuffer : buffer) {
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

		synchronized (this) {
			this.notify();
		}
	}
}
