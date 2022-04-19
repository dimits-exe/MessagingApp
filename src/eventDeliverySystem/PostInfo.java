package eventDeliverySystem;

import eventDeliverySystem.Post.DataType;

/**
 * Contains information about a Post. It is shared among the Packets of a Post.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
class PostInfo {

	private final DataType type;
	private final Profile  poster;
	private final String   fileExtension;
	private final Topic    topic;
	private final long     id;

	/**
	 * Constructs a new PostInfo.
	 *
	 * @param type          the Type of the data of the PostInfo
	 * @param poster        the Poster of the PostInfo
	 * @param fileExtension the file extension associated with the data of the
	 *                      PostInfo. Must be {@code null} if
	 *                      {@code type == DataType.TEXT}.
	 * @param topic         the Topic of the PostInfo
	 * @param id            the id of the PostInfo
	 */
	public PostInfo(DataType type, Profile poster, String fileExtension, Topic topic,
	        long id) {
		if ((type == DataType.TEXT) && (fileExtension != null))
			throw new IllegalArgumentException("DataType TEXT requires 'null' file extension");

		this.type = type;
		this.poster = poster;
		this.fileExtension = fileExtension;
		this.topic = topic;
		this.id = id;
	}

	/**
	 * Returns the poster.
	 *
	 * @return the poster
	 */
	public Profile getPoster() {
		return poster;
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
	 * Returns this RawData's topic.
	 *
	 * @return the topic
	 */
	public Topic getTopic() {
		return topic;
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
