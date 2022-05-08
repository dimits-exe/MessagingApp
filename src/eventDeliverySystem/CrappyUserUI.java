package eventDeliverySystem;

import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * TODO
 *
 * @author Alex Mandelias
 */
class CrappyUserUI extends JFrame {

	public static void main(String[] args) throws IOException {
		CrappyUserUI ui = new CrappyUserUI(true, args[0].equals("alex"));
		ui.setVisible(true);
	}

	private final User user;

	// TODO: remove
	class UserUISub {
		public void notify(String topicName) {
			JOptionPane.showMessageDialog(CrappyUserUI.this,
			        String.format("YOU HAVE A NEW MESSAGE AT '%s'", topicName));
		}
	}

	public CrappyUserUI(boolean existing, boolean alex) throws IOException {
		super(alex ? "alex" : "dimits");
		if (alex)
			user = User.forAlex(existing, false);
		else
			user = User.forDimits(existing, false);

		user.setUserUISub(new UserUISub());

		setSize(800, 600);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		JPanel main = new JPanel(new GridLayout(2, 2));
		main.add(getPostPanel());
		main.add(getCreateTopicPanel());
		main.add(getPullPanel());
		main.add(getListenForTopicPanel());
		add(main);
	}

	private final JPanel getPostPanel() {
		JPanel main = new JPanel();
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

		JTextField file_text = new JTextField(20);
		JTextField topicName = new JTextField(20);
		JButton    postFile  = new JButton("File");
		JButton    postText  = new JButton("Text");

		postFile.addActionListener((e) -> {
			try {
				User user1    = CrappyUserUI.this.user;
				File file     = new File(file_text.getText());
				long posterId = user1.getCurrentProfile().getId();
				Post post     = Post.fromFile(file, posterId);
				user1.post(post, topicName.getText());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});

		postText.addActionListener((e) -> {
			User   user1    = CrappyUserUI.this.user;
			String text     = file_text.getText();
			long   posterId = user1.getCurrentProfile().getId();
			Post   post     = Post.fromText(text, posterId);
			user1.post(post, topicName.getText());
		});

		main.add(file_text);
		main.add(topicName);
		main.add(postFile);
		main.add(postText);

		return main;
	}

	private final JPanel getCreateTopicPanel() {
		JPanel main = new JPanel();
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

		JTextField topicName   = new JTextField(20);
		JButton    createTopic = new JButton("Create");

		createTopic.addActionListener((e) -> {
			User user1 = CrappyUserUI.this.user;
			try {
				user1.createTopic(topicName.getText());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});

		main.add(topicName);
		main.add(createTopic);

		return main;
	}

	private final JPanel getPullPanel() {
		JPanel main = new JPanel();
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

		JTextField topicName   = new JTextField(20);
		JButton    pull      = new JButton("Pull");

		pull.addActionListener((e) -> {
			User user1 = CrappyUserUI.this.user;
			try {
				user1.pull(topicName.getText());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});

		main.add(topicName);
		main.add(pull);

		return main;
	}

	private final JPanel getListenForTopicPanel() {
		JPanel main = new JPanel();
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

		JTextField topicName   = new JTextField(20);
		JButton    lsitenForTopic = new JButton("Listen For Topic");

		lsitenForTopic.addActionListener((e) -> {
			User user1 = CrappyUserUI.this.user;
			try {
				user1.listenForTopic(topicName.getText());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});

		main.add(topicName);
		main.add(lsitenForTopic);

		return main;
	}
}
