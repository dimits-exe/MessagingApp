package eventDeliverySystem;

import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A thread that sends a series of Posts to many connections
 * simultaneously.
 *
 */
class MulticastPushThread extends Thread {
	private final List<Post> posts;
	private final Set<Thread> workerThreads;
	
	public MulticastPushThread(Set<ObjectOutputStream> outStreams, List<Post> posts) {
		this.posts = posts;
		this.workerThreads = new HashSet<>();
		for(ObjectOutputStream out : outStreams) {
			workerThreads.add(new IterativePushThread(out, posts));
		}
		
	}
	
	@Override
	public void run() {
		for(Thread t : workerThreads) {
			t.start();
		}
		
		try {
			for(Thread t : workerThreads) {
				t.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}