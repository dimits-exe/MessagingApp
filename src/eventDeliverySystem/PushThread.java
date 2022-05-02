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
	private final boolean            keepAlive;

	private boolean success, start, end;

	/**
	 * Constructs the Thread that, when run, will write some Posts to a stream.
	 *
	 * @param stream    the output stream to which to write the Posts
	 * @param posts     the Posts to write
	 * @param keepAlive {@code true} if the PullThread associated with pulling the
	 *                  data should not terminate after reading the data that this
	 *                  PushThread pushes, {@code false} otherwise.
	 */
	public PushThread(ObjectOutputStream stream, List<Post> posts, boolean keepAlive) {
		super("PushThread-" + posts.size() + "-" + keepAlive);
		oos = stream;
		this.posts = posts;
		this.keepAlive = keepAlive;
		success = start = end = false;
	}

	@Override
	public void run() {
		LG.sout("%s#run()", getName());
		LG.in();
		start = true;

		try /* (oos) */ {

			final int postCount = keepAlive ? Integer.MAX_VALUE : posts.size();
			LG.sout("postCount=%d", postCount);
			LG.in();
			oos.writeInt(postCount);

			for (Post post : posts) {
				final PostInfo postInfo = post.getPostInfo();
				LG.sout("postInfo=%s", postInfo);
				oos.writeObject(postInfo);

				final Packet[] packets = Packet.fromPost(post);
				for (Packet packet : packets)
					oos.writeObject(packet);
			}

			LG.out();

			success = true;

		} catch (IOException e) {
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