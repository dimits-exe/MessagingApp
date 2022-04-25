package eventDeliverySystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;


/**
 * A wrapper class that receives multiple posts by delegating PullThreads.
 *
 */
class IterativePullThread extends Thread {
	private final ObjectInputStream in;
	private final Topic topic;
	
	public IterativePullThread(ObjectInputStream in, Topic topic) {
		this.in = in;
		this.topic = topic;
	}
	
	@Override
	public void run() {
		try {
			// read how many posts will be transmitted
			final int postCount = in.read();
			
			//start receiving posts
			for(int i=0; i< postCount; i++) {
				PullThread pullThread = new PullThread(in, topic);
				pullThread.start();
				pullThread.join();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
