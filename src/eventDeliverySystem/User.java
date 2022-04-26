package eventDeliverySystem;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import eventDeliverySystem.Post.DataType;

@SuppressWarnings("javadoc")
public class User {

	public User() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		int port = 29773;

		Profile poster1 = new Profile("alex");
		sout("poster");

		Publisher p = new Publisher("192.168.1.145", port);
		sout("publisher");

		Set<Topic> t = new HashSet<>(List.of(new Topic("opa")));
		sout("topic");

		Consumer c = new Consumer("192.168.1.145", port, t);
		sout("consumer");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedImage img   = ImageIO.read(new File("C:\\users\\alexm\\Desktop\\epic-cat.png"));
		ImageIO.write(img, "jpg", baos);
		byte[] bytes = baos.toByteArray();
		Post   post1 = new Post(bytes, DataType.IMAGE, poster1, ".png", "opa");
		p.push(post1);

		sout("post");

		List<Post> posts = c.pull("opa");
		Post       cat   = posts.get(0);

		sout("pull");

		byte[]               mpitia = cat.getData();
		ByteArrayInputStream bais   = new ByteArrayInputStream(mpitia);
		BufferedImage        image  = ImageIO.read(bais);
		JFrame               frame  = new JFrame();
		JPanel               panel  = new JPanel();
		ImageIcon            ii     = new ImageIcon(mpitia);
		JLabel               label  = new JLabel(ii);
		panel.add(label);
		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
		sout("end");
	}

	private static void sout(String format, Object... args) {
		System.out.printf(format + "\n", args);
		System.out.flush();
	}
}
