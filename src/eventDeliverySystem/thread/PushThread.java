package eventDeliverySystem.thread;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import eventDeliverySystem.LG;
import eventDeliverySystem.datastructures.Packet;
import eventDeliverySystem.datastructures.PostInfo;

/**
 * A Thread that writes some Posts to a stream.
 *
 * @author Alex Mandelias
 */
public class PushThread extends Thread {

	/**
	 * Defines the different Protocol' used to push data.
	 *
	 * @author Alex Mandelias
	 */
	public enum Protocol {

		/** Tell Pull Thread to receive a set amount of data and stop */
		NORMAL,

		/**
		 * Tell Pull Thread to always wait to receive data. Subsequent Push Threads to
		 * the same output stream should use the 'WITHOUT_COUNT' protocol.
		 */
		KEEP_ALIVE,

		/**
		 * Just send the data, don't send the amount of data. Should be used in
		 * conjunction with 'KEEP_ALIVE', for pushing after the Pull Thread has started.
		 */
		WITHOUT_COUNT;
	}

	private final ObjectOutputStream  oos;
	private final List<PostInfo>      postInfos;
	private final Map<Long, Packet[]> packets;
	private final Protocol            protocol;

	private boolean success, start, end;

	/**
	 * Constructs the Thread that, when run, will write some Posts to a stream.
	 *
	 * @param stream    the output stream to which to write the Posts
	 * @param postInfos the PostInfo objects to write to the stream
	 * @param packets   the array of Packets to write for each PostInfo object
	 * @param protocol  the protocol to use when pushing, which alters the behaviour
	 *                  of the Pull Thread
	 *
	 * @see Protocol
	 */
	public PushThread(ObjectOutputStream stream, List<PostInfo> postInfos,
	        Map<Long, Packet[]> packets, Protocol protocol) {
		super("PushThread-" + postInfos.size() + "-" + protocol);
		oos = stream;
		this.postInfos = postInfos;
		this.packets = packets;
		this.protocol = protocol;
		success = start = end = false;
	}

	@Override
	public void run() {
		LG.sout("%s#run()", getName());
		LG.in();
		start = true;

		try /* (oos) */ {

			LG.sout("protocol=%s, posts.size()=%d", protocol, postInfos.size());
			LG.in();

			if (protocol != Protocol.WITHOUT_COUNT) {
				final int postCount;
				if (protocol == Protocol.KEEP_ALIVE)
					postCount = Integer.MAX_VALUE;
				else
					postCount = postInfos.size();

				oos.writeInt(postCount);
			}

			for (final PostInfo postInfo : postInfos) {
				LG.sout("postInfo=%s", postInfo);
				oos.writeObject(postInfo);

				final Packet[] packetArray = packets.get(postInfo.getId());
				for (final Packet packet : packetArray)
					oos.writeObject(packet);
			}

			LG.out();

			success = true;

		} catch (final IOException e) {
			System.err.printf("IOException in PushThread#run()%n");
			e.printStackTrace();
			success = false;
		}

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
