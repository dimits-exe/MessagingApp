package eventDeliverySystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("javadoc")
public class UserTest {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		User user = User.forAlex();
		user.createTopicLocal("poggers");

		Post p1 = Post.fromFile(new File("C:\\Users\\alexm\\Desktop\\epic-cat.png"), 1);
		Post p2 = Post.fromFile(new File("C:\\Users\\alexm\\Desktop\\epic-cat2.png"), 1);
		List<Post> toPost = List.of(p1, p2);
		System.out.println("posting:");
		for (Post post : toPost) {
			System.out.println(post);
		}

		user.pullLocal("poggers", List.of(p1, p2));

		System.out.println("reading:");
		for (Topic topic : user.user.getTopics().values()) {
			System.out.println(topic);
			for (Post post : topic.getAllPosts()) {
				System.out.println(post);
			}
		}
	}
}
