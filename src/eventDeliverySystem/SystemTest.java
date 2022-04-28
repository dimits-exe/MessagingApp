package eventDeliverySystem;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import eventDeliverySystem.Post.DataType;

@SuppressWarnings("javadoc")
public class SystemTest {
	
	@SuppressWarnings("resource")
	public static void main(String[] args)
	        throws NumberFormatException, IOException {
		String ip = "192.168.2.12";
		int port = 29973;
		Topic newTopic = new Topic("opa");
	
		Profile poster1 = new Profile("alex");
		sout("poster");
	
		Publisher p = new Publisher(ip, port);
		sout("publisher");
	
		Set<Topic> t = new HashSet<>(List.of(newTopic));
		sout("topic");

		Post post1 = loadImage(Paths.get("C:\\Users\\user\\Pictures\\discord\\ilektra.PNG"), ".png", poster1, newTopic.getName());
		Post post2 = loadImage(Paths.get("C:\\Users\\user\\Pictures\\discord\\sus.png"), ".png", poster1, newTopic.getName());
		sout("img");
		
		p.createTopic(newTopic.getName());
		p.push(post1);
		p.push(post2);
		sout("post");
	
		Consumer c = new Consumer(ip, port, t);
		sout("consumer");
		
		System.out.println("Type anything lol:");
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
		List<Post> posts = c.pull(newTopic.getName());
		sout("Pulled " + posts.size() + " posts");
		
		JFrame frame = new JFrame();
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		for(Post post : posts) {
			mainPanel.add(getImageRepresantation(post));
		}
		
		frame.add(mainPanel);
		frame.setSize(1000, 1000);
		frame.setResizable(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
		sout("end");
	}
	
	private static Post loadImage(Path path, String fileExtension, Profile poster1, String topicName) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedImage         img  = ImageIO.read(path.toFile());
		ImageIO.write(img, "png", baos);
		byte[] bytes = baos.toByteArray();
		Post   post1 = new Post(bytes, DataType.IMAGE, poster1.getId(), fileExtension, topicName);
		sout("img");
	
		return post1;
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
