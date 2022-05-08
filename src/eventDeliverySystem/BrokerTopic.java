package eventDeliverySystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * An extension of the Abstract Topic that stores data as required by Brokers.
 * The Posts are stored disassembled as PostInfo and Packet objects.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
public class BrokerTopic extends AbstractTopic {

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
		this.postInfoList = Collections.synchronizedList(new LinkedList<>());
		this.packetsPerPostInfoMap = new HashMap<>();
		this.indexPerPostInfoId = new HashMap<>();
	}

	@Override
	public void postHook(PostInfo postInfo) {
		postInfoList.add(postInfo);

		long         postId     = postInfo.getId();
		List<Packet> packetList = Collections.synchronizedList(new LinkedList<>());

		synchronized (packetsPerPostInfoMap) {
			packetsPerPostInfoMap.put(postId, packetList);
		}

		synchronized (indexPerPostInfoId) {
			indexPerPostInfoId.put(postId, postInfoList.size() - 1);
		}
	}

	@Override
	public void postHook(Packet packet) {
		long postId   = packet.getPostId();

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

		// broker is not persistent, consumer may have posts from previous session
		if (!indexPerPostInfoId.containsKey(postId)) {
			return;
		}

		int index = indexPerPostInfoId.get(postId);
		ListIterator<PostInfo> postInfoIter;
		for (postInfoIter = postInfoList.listIterator(index); postInfoIter.hasNext();) {

			PostInfo curr = postInfoIter.next();

			emptyPostInfoList.add(curr);

			long id = curr.getId();
			List<Packet> ls = packetsPerPostInfoMap.get(id);
			emptyPacketsPerPostInfoMap.put(id, ls.toArray(new Packet[ls.size()]));
		}
	}
}
