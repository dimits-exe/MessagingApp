package eventDeliverySystem;

import java.io.Serializable;

import eventDeliverySystem.Post.DataType;

/**
 * Contains information about a Post. It is shared among the Packets of a Post.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
class PostInfo implements Serializable {
	private static final long serialVersionUID = 1;

	private final DataType type;
	private final long     posterId;
	private final String   fileExtension;
	private final String   topicName;
	private final long     id;

	/**
	 * Constructs a new PostInfo.
	 *
	 * @param type          the Type of the data of the PostInfo
	 * @param posterId      the ID of the Poster of the Post
	 * @param fileExtension the file extension associated with the data of the
	 *                      PostInfo. Must be {@code null} if
	 *                      {@code type == DataType.TEXT}.
	 * @param topicName     the name of the PostInfo
	 * @param id            the id of the PostInfo
	 */
	public PostInfo(DataType type, long posterId, String fileExtension, String topicName,
	        long id) {
		if ((type == DataType.TEXT) && (fileExtension != null))
			throw new IllegalArgumentException("DataType TEXT requires 'null' file extension");

		this.type = type;
		this.posterId = posterId;
		this.fileExtension = fileExtension;
		this.topicName = topicName;
		this.id = id;
	}

	/**
	 * Returns the posterId.
	 *
	 * @return the posterId
	 */
	public long getPosterId() {
		return posterId;
	}

	/**
	 * Returns the type.
	 *
	 * @return the type
	 */
	public DataType getType() {
		return type;
	}

	/**
	 * Returns the fileExtension.
	 *
	 * @return the fileExtension
	 */
	public String getFileExtension() {
		return fileExtension;
	}

	/**
	 * Returns the topicName.
	 *
	 * @return the topicName
	 */
	public String getTopicName() {
		return topicName;
	}

	/**
	 * Returns the id.
	 *
	 * @return the id
	 */
	public long getId() {
		return id;
	}
}
