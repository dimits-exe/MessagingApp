package eventDeliverySystem.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import eventDeliverySystem.datastructures.AbstractTopic;
import eventDeliverySystem.datastructures.Packet;
import eventDeliverySystem.datastructures.PostInfo;

/**
 * An extension of the Abstract Topic that stores data as required by Brokers.
 * The Posts are stored disassembled as PostInfo and Packet objects.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
class BrokerTopic extends AbstractTopic {

	private final List<PostInfo>          postInfoList;
	private final Map<Long, List<Packet>> packetsPerPostInfoMap;
	private final Map<Long, Integer>      indexPerPostInfoId;

	/**
	 * Constructs an empty BrokerTopic.
	 *
	 * @param name the name of the new BrokerTopic
	 */
	public BrokerTopic(String name) {
		super(name);
		postInfoList = Collections.synchronizedList(new LinkedList<>());
		packetsPerPostInfoMap = new HashMap<>();
		indexPerPostInfoId = new HashMap<>();
	}

	@Override
	public void postHook(PostInfo postInfo) {
		postInfoList.add(postInfo);

		final long         postId     = postInfo.getId();
		final List<Packet> packetList = Collections.synchronizedList(new LinkedList<>());

		synchronized (packetsPerPostInfoMap) {
			packetsPerPostInfoMap.put(postId, packetList);
		}

		synchronized (indexPerPostInfoId) {
			indexPerPostInfoId.put(postId, postInfoList.size() - 1);
		}
	}

	@Override
	public void postHook(Packet packet) {
		final long postId = packet.getPostId();

		List<Packet> packetList;
		synchronized (packetsPerPostInfoMap) {
			packetList = packetsPerPostInfoMap.get(postId);
		}

		packetList.add(packet);
	}

	/**
	 * Fills the List and the Map with all of the PostInfo and Packet objects in
	 * this Topic. The first PostInfo object in the list is the earlier and the last
	 * is the latest.
	 *
	 * @param emptyPostInfoList          the empty list where the PostInfo objects
	 *                                   will be added
	 * @param emptyPacketsPerPostInfoMap the empty map where the Packets of every
	 *                                   PostInfo object will be added
	 */
	public void getAllPosts(List<PostInfo> emptyPostInfoList,
	        Map<Long, Packet[]> emptyPacketsPerPostInfoMap) {
		emptyPostInfoList.addAll(postInfoList);
		packetsPerPostInfoMap.forEach(
		        (id, ls) -> emptyPacketsPerPostInfoMap.put(id, ls.toArray(new Packet[ls.size()])));
	}

	/**
	 * Fills the List and the Map with all of the PostInfo and Packet objects in
	 * this Topic starting from a certain PostInfo object. The first PostInfo object
	 * in the list is the earlier and the last is the latest.
	 *
	 * @param postId                     the id of the PostInfo after which to
	 *                                   return the object
	 * @param emptyPostInfoList          the empty list where the PostInfo objects
	 *                                   will be added
	 * @param emptyPacketsPerPostInfoMap the empty map where the Packets of every
	 *                                   PostInfo object will be added
	 */
	public void getPostsSince(long postId, List<PostInfo> emptyPostInfoList,
	        Map<Long, Packet[]> emptyPacketsPerPostInfoMap) {

		if (postId == AbstractTopic.FETCH_ALL_POSTS)
			getAllPosts(emptyPostInfoList, emptyPacketsPerPostInfoMap);

		// broker is not persistent, consumer may have posts from previous session
		if (!indexPerPostInfoId.containsKey(postId))
			return;

		final int              index = indexPerPostInfoId.get(postId);
		ListIterator<PostInfo> postInfoIter;
		for (postInfoIter = postInfoList.listIterator(index); postInfoIter.hasNext();) {

			final PostInfo curr = postInfoIter.next();

			emptyPostInfoList.add(curr);

			final long         id = curr.getId();
			final List<Packet> ls = packetsPerPostInfoMap.get(id);
			emptyPacketsPerPostInfoMap.put(id, ls.toArray(new Packet[ls.size()]));
		}
	}
}
