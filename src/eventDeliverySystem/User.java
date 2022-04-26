package eventDeliverySystem;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import eventDeliverySystem.Post.DataType;

@SuppressWarnings("javadoc")
public class User {

	public User() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args)
	        throws NumberFormatException, IOException {
		int port = 29863;

		Profile poster1 = new Profile("alex");
		sout("poster");

		Publisher p = new Publisher("192.168.1.145", port);
		sout("publisher");

		Set<Topic> t = new HashSet<>(List.of(new Topic("opa")));
		sout("topic");

		// Consumer c = new Consumer("192.168.1.145", port, t);
		// sout("consumer");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedImage         img  = ImageIO
		        .read(new File("C:\\users\\alexm\\Desktop\\epic-cat2.png"));
		ImageIO.write(img, "jpg", baos);
		byte[] bytes = baos.toByteArray();
		Post   post1 = new Post(bytes, DataType.IMAGE, poster1.getId(), ".png", "opa");
		sout("img");

		p.push(post1);
		sout("post");

		Consumer c = new Consumer("192.168.1.145", port, t);
		sout("consumer");
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
		List<Post> posts = c.pull("opa");
		Post       cat   = posts.get(0);

		sout("pull");

		byte[]               mpitia = cat.getData();
		ByteArrayInputStream bais   = new ByteArrayInputStream(mpitia);
		BufferedImage        image  = ImageIO.read(bais);
		Image                img1   = image.getScaledInstance(800, 600, 0);
		JFrame frame = new JFrame();
		JPanel panel = new JPanel() {
							@Override
							public void paintComponent(Graphics g) {
								super.paintComponent(g);
											g.drawImage(img1, 0, 0, null);
							}
						};

		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
		frame.add(panel);
		sout("end");






		/*
		@foff
		baos = new ByteArrayOutputStream();
		img = ImageIO.read(new File("C:\\users\\alexm\\Desktop\\epic-cat2.png"));
		ImageIO.write(img, "jpg", baos);
		bytes = baos.toByteArray();
		post1 = new Post(bytes, DataType.IMAGE, poster1.getId(), ".png", "opa");
		p.push(post1);
		sout("img");

		sout("consumer");
		scanner.nextLine();
		posts = c.pull("opa");
		sout("%d", posts.size());
		cat = posts.get(1);

		sout("pull");

		mpitia = cat.getData();
		bais = new ByteArrayInputStream(mpitia);
		image = ImageIO.read(bais);
		Image  img2   = image.getScaledInstance(800, 600, 0);
		JFrame frame1 = new JFrame();
		JPanel panel1 = new JPanel() {
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
								g.drawImage(img2, 0, 0, null);
			}
		};

		frame1.setSize(800, 600);
		frame1.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame1.setVisible(true);
		frame1.add(panel1);
		sout("end");
		@fon
		*/
	}

	private static void sout(String format, Object... args) {
		System.out.printf(format + "\n", args);
		System.out.flush();
	}
}
