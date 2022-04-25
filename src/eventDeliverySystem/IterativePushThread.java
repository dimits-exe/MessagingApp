package eventDeliverySystem;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * A wrapper class that iterates over a list of Posts and delegates
 * PushThreads for every single one of them.
 *
 */
class IterativePushThread extends Thread {
	private final List<Post> posts;
	private final ObjectOutputStream out;
	
	/**
	 * Create a new Thread that when run will send all the posts to the output stream.
	 * @param out the output stream
	 * @param posts an ordered list of all the posts
	 */
	public IterativePushThread(ObjectOutputStream out, List<Post> posts) {
		this.posts = posts;
		this.out = out;
	}
	
	@Override
	public void run() {
		try {
			// send length of post list to be sent
			out.write(posts.size());
			
			//start sending posts
			for(Post post: posts) {
				Thread pullThread = new PushThread(Packet.fromPost(post), out);
				pullThread.start();
				pullThread.join();
			} 
		} catch (IOException e1) {
			throw new UncheckedIOException(e1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
