package eventDeliverySystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("javadoc")
public class UserTest {

	static synchronized public void main(String[] args)
	        throws FileNotFoundException, IOException, InterruptedException {

		boolean local = false;
		User    user  = User.forAlex(false, local);

		if (local) {
			user.createTopicLocal("cats");
			user.createTopicLocal("cats2");

			Post p1 = Post.fromFile(new File("C:\\Users\\alexm\\Desktop\\epic-cat.png"), 1);
			Post p2 = Post.fromFile(new File("C:\\Users\\alexm\\Desktop\\epic-cat2.png"), 1);

			user.postLocal(p1, "cats");
			user.postLocal(p2, "cats");
			user.postLocal(p1, "cats2");

			Topic topic  = new Topic("cats");
			Topic topic2 = new Topic("cats2");
			user.listenForTopicLocal(topic);
			user.listenForTopicLocal(topic2);

			user.pullLocal("cats", List.of(p1, p2));
			user.pullLocal("cats2", List.of(p1));
		} else {
			if (!user.createTopic("cats"))
				System.err.printf("Failed to create topic 'cats'");
			if (!user.createTopic("cats2"))
				System.err.printf("Failed to create topic 'cats2'");

			Post p1 = Post.fromFile(new File("C:\\Users\\alexm\\Desktop\\epic-cat.png"),
			        user.getCurrentProfile().getId());
			Post p2 = Post.fromFile(new File("C:\\Users\\alexm\\Desktop\\epic-cat2.png"),
			        user.getCurrentProfile().getId());

			user.post(p1, "cats");
			user.post(p2, "cats");
			user.post(p1, "cats2");

			Object m = new Object();
			System.out.println("sleep");
			synchronized (m) {
				m.wait(10000);
			}
			System.out.println("wake");

			Topic topic = new Topic("cats");
			Topic topic2 = new Topic("cats2");
			user.listenForTopic("cats");
			user.listenForTopic("cats2");

			System.out.println("sleep");
			synchronized (m) {
				m.wait(10000);
			}
			System.out.println("wake");

			user.pull("cats");
			user.pull("cats2");
			LG.sout("user end");

		}
	}
}
