package com.example.messagingapp.eventDeliverySystem;

import com.example.messagingapp.eventDeliverySystem.client.Consumer;
import com.example.messagingapp.eventDeliverySystem.client.Publisher;
import com.example.messagingapp.eventDeliverySystem.datastructures.Post;
import com.example.messagingapp.eventDeliverySystem.filesystem.FileSystemException;
import com.example.messagingapp.eventDeliverySystem.filesystem.Profile;
import com.example.messagingapp.eventDeliverySystem.filesystem.ProfileFileSystem;
import com.example.messagingapp.eventDeliverySystem.server.ServerException;
import com.example.messagingapp.eventDeliverySystem.util.LG;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;


/**
 * A class that manages the actions of the user by communicating with the server
 * and retrieving / committing posts to the file system.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
public class User implements Serializable, IUser {

	private final ProfileFileSystem profileFileSystem;
	private final ISubscriber 		userSub;
	private Profile                 currentProfile;

	private final Publisher publisher;
	private final Consumer  consumer;

	/**
	 * Retrieve the user's data and the saved posts, establish connection to the
	 * server and prepare to receive and send posts.
	 *
	 * @param serverIP              the IP of the server
	 * @param serverPort            the port of the server
	 * @param profilesRootDirectory the root directory of all the Profiles in the
	 *                              file system
	 * @param profileName           the name of the existing profile
	 * @return the new User
	 * @throws ServerException      if the connection to the server fails
	 * @throws FileSystemException  if an I/O error occurs while interacting with
	 *                              the file system
	 * @throws UnknownHostException if no IP address for the host could be found, or
	 *                              if a scope_id was specified for a global
	 *                              IPv6address while resolving the defaultServerIP.
	 */
	public static User loadExisting(ISubscriber userSub, String serverIP, int serverPort, Path profilesRootDirectory,
							 String profileName) throws ServerException, FileSystemException, UnknownHostException {
		final User user = new User(userSub, serverIP, serverPort, profilesRootDirectory);
		user.switchToExistingProfile(profileName);
		return user;
	}

	/**
	 * Creates a new User in the file system and returns a new User object.
	 *
	 * @param serverIP              the IP of the server
	 * @param serverPort            the port of the server
	 * @param profilesRootDirectory the root directory of all the Profiles in the
	 *                              file system
	 * @param name                  the name of the new Profile
	 * @return the new User
	 * @throws ServerException      if the connection to the server fails
	 * @throws FileSystemException  if an I/O error occurs while interacting with
	 *                              the file system
	 * @throws UnknownHostException if no IP address for the host could be found, or
	 *                              if a scope_id was specified for a global
	 *                              IPv6address while resolving the defaultServerIP.
	 */
	public static User createNew(ISubscriber userSub, String serverIP, int serverPort, Path profilesRootDirectory,
						  String name) throws ServerException, FileSystemException, UnknownHostException {
		final User user = new User(userSub, serverIP, serverPort, profilesRootDirectory);
		user.switchToNewProfile(name);
		return user;
	}


	private User(ISubscriber userSub, String serverIP, int port, Path profilesRootDirectory)
	        throws FileSystemException, UnknownHostException {
		this.userSub = userSub;
		profileFileSystem = new ProfileFileSystem(profilesRootDirectory);

		publisher = new Publisher(serverIP, port, userSub);
		consumer = new Consumer(serverIP, port, userSub);

	}

	@Override
	public Profile getCurrentProfile() {
		return currentProfile;
	}

	@Override
	public void switchToNewProfile(String profileName) throws ServerException, FileSystemException {
		currentProfile = profileFileSystem.createNewProfile(profileName);
		consumer.setTopics(new HashSet<>(currentProfile.getTopics().values()));
	}

	@Override
	public void switchToExistingProfile(String profileName)
	        throws ServerException, FileSystemException {
		currentProfile = profileFileSystem.loadProfile(profileName);
		consumer.setTopics(new HashSet<>(currentProfile.getTopics().values()));
	}

	@Override
	public void post(Post post, String topicName) {
		publisher.push(post, topicName);
	}

	@Override
	public boolean createTopic(String topicName) throws ServerException, FileSystemException {
		LG.sout("User#createTopic(%s)", topicName);
		LG.in();
		final boolean success = publisher.createTopic(topicName);
		LG.sout("success=%s", success);
		if (success)
			listenForNewTopic(topicName);

		LG.out();
		return success;
	}

	@Override
	public void pull(String topicName) throws FileSystemException {
		LG.sout("User#pull from Topic '%s'", topicName);
		LG.in();
		final List<Post> newPosts = consumer.pull(topicName); // sorted from earliest to latest
		LG.sout("newPosts=%s", newPosts);
		currentProfile.updateTopic(topicName, newPosts);

		for (final Post post : newPosts) {
			LG.sout("Saving Post '%s'", post);
			profileFileSystem.savePost(post, topicName);
		}
		LG.out();
	}

	@Override
	public void listenForNewTopic(String topicName) throws ServerException, FileSystemException {
		consumer.listenForNewTopic(topicName);
		currentProfile.addTopic(topicName);
		profileFileSystem.createTopic(topicName);
	}

	@Override
	public ISubscriber getSubscriber() {
		return userSub;
	}

}
