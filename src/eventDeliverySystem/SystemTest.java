package eventDeliverySystem;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

@SuppressWarnings("javadoc")
public class SystemTest {


	@SuppressWarnings("resource")
	public static void main(String[] args) throws NumberFormatException, IOException {
		String ip       = "192.168.2.12";
		int    port     = 29973;
		Topic  newTopic = new Topic("opa");

		Profile poster1 = new Profile("alex");
		sout("poster");

		Publisher p = new Publisher(ip, port);
		sout("publisher");

		Set<Topic> t = new HashSet<>(List.of(newTopic));
		sout("topic");

		File img1  = new File("C:\\Users\\user\\Pictures\\discord\\ilektra.PNG");
		File img2  = new File("C:\\Users\\user\\Pictures\\discord\\sus.PNG");
		Post post1 = Post.fromFile(img1, poster1.getId());
		Post post2 = Post.fromFile(img2,
		        poster1.getId());
		sout("img");

		p.createTopic(newTopic.getName());
		p.push(post1, newTopic.getName());
		p.push(post2, newTopic.getName());
		sout("post");

		Consumer c = new Consumer(ip, port, null);
		sout("consumer");

		System.out.println("Type anything lol:");
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
		List<Post> posts = c.pull(newTopic.getName());
		sout("Pulled " + posts.size() + " posts");

		JFrame frame     = new JFrame();
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		for (Post post : posts) { mainPanel.add(getImageRepresantation(post)); }

		frame.add(mainPanel);
		frame.setSize(1000, 1000);
		frame.setResizable(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
		sout("end");
	}

	private static JPanel getImageRepresantation(Post post) throws IOException {
		byte[]               mpitia = post.getData();
		ByteArrayInputStream bais   = new ByteArrayInputStream(mpitia);
		BufferedImage        image  = ImageIO.read(bais);
		Image                img1   = image.getScaledInstance(200, 150, 0);

		@SuppressWarnings("serial")
		JPanel panel = new JPanel() {

			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(img1, 0, 0, null);
			}
		};

		return panel;
	}


	private static void sout(String format, Object... args) {
		System.out.printf(format + "\n", args);
		System.out.flush();
	}
}
