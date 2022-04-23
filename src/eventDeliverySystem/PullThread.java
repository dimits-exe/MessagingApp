package eventDeliverySystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * A Thread that continuously reads data from an input stream, re-assembles them
 * and adds them on a provided list.
 */
class PullThread extends Thread {

	private final ObjectInputStream stream;
	private final int postCount;
	private boolean                 success, start, end;

	private final Topic topic;

	/**
	 * Constructs the Thread that, when run, will read data from the stream.
	 *
	 * @param stream the input stream from which to read the data
	 * @param topic the topic in which the new posts will be added
	 * @param postCount the number of posts to be read
	 */
	public PullThread(ObjectInputStream stream, Topic topic, int postCount) {
		this.stream = stream;
		success = start = end = false;
		this.topic = topic;
		this.postCount = postCount;
	}

	@Override
	public void run() {
		start = true;

		final List<Packet> postFragments = new LinkedList<>();

		try {
			for (int i = 0; i < postCount; i++) {
				
				//read Post header
				PostInfo postInfo;
				try {
					postInfo = (PostInfo) stream.readObject();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					return;
				}
				
				//start reading Post data
				Packet packet;
				do {
					try {
						packet = (Packet) stream.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}
					postFragments.add(packet);
				} while (!packet.isFinal());

				final Packet[] temp    = new Packet[postFragments.size()];
				final Packet[] packets = postFragments.toArray(temp);
				final Post     post    = Post.fromPackets(packets, postInfo);
				topic.post(post);
			}

			success = true;

		} catch (IOException e) {
			System.err.printf("IOException while receiving packets from actual broker%n");
			success = false; // maybe rename to "finish" because an IOException is NOT a success lmfao
		}

		end = true;
	}

	
	//why does this method only return true? Either make it a void checkIfComplete() or return end;
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