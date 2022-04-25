package eventDeliverySystem;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * A Thread that writes some Posts to a stream.
 *
 * @author Alex Mandelias
 */
class PushThread extends Thread {

	private final ObjectOutputStream oos;
	private final List<Post>         posts;

	private boolean success, start, end;

	/**
	 * Constructs the Thread that, when run, will write some Posts to a stream.
	 *
	 * @param stream the output stream to which to write the Posts
	 * @param posts  the Posts to write
	 */
	public PushThread(ObjectOutputStream stream, List<Post> posts) {
		oos = stream;
		this.posts = posts;
		success = start = end = false;
	}

	@Override
	public void run() {
		start = true;

		try (oos) {

			final int postCount = posts.size();
			oos.writeInt(postCount);

			for (Post post : posts) {
				final PostInfo postInfo = post.getPostInfo();
				oos.writeObject(postInfo);

				final Packet[] packets = Packet.fromPost(post);
				for (Packet packet : packets)
					oos.writeObject(packet);
			}

			success = true;

		} catch (IOException e) {
			System.err.printf("IOException while sending packets to actual broker%n");
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