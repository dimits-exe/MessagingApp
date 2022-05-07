package eventDeliverySystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * TODO
 *
 * @author Alex Mandelias
 */
public class BrokerTopic extends AbstractTopic {

	private final List<PostInfo>          postInfoList;
	private final Map<Long, List<Packet>> packetsPerPostInfoMap;

	/**
	 * TODO
	 *
	 * @param name
	 */
	public BrokerTopic(String name) {
		super(name);
		this.postInfoList = Collections.synchronizedList(new LinkedList<>());
		this.packetsPerPostInfoMap = new HashMap<>();
	}

	@Override
	public void post(PostInfo postInfo) {
		postInfoList.add(postInfo);

		long         postId     = postInfo.getId();
		List<Packet> packetList = Collections.synchronizedList(new LinkedList<>());

		synchronized (packetsPerPostInfoMap) {
			packetsPerPostInfoMap.put(postId, packetList);
		}
	}

	@Override
	public void post(Packet packet) {
		long postId   = packet.getPostId();

		List<Packet> packetList;
		synchronized (packetsPerPostInfoMap) {
			packetList = packetsPerPostInfoMap.get(postId);
		}

		packetList.add(packet);
	}

	public void getAllPosts(List<PostInfo> emptyPostInfoList,
	        Map<Long, Packet[]> emptyPacketsPerPostInfoMap) {
		emptyPostInfoList.addAll(postInfoList);
		packetsPerPostInfoMap.forEach(
		        (id, ls) -> emptyPacketsPerPostInfoMap.put(id, ls.toArray(new Packet[ls.size()])));
	}
}
