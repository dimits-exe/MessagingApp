package eventDeliverySystem;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * An immutable pair of <IPAddress, Port> representing a unique connection
 * in the web.
 *
 */
final class ConnectionInfo implements Serializable {

	private static final long serialVersionUID = -3742145900403155967L;
	
	private final InetAddress address;
	private final int port;
	
	/**
	 * Construct a new info object with the connection details of the host.
	 * @param address the IP address of the host
	 * @param port the port number of the host
	 */
	public ConnectionInfo(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
	
	/**
	 * Get the IP address of the connection.
	 * @return the address
	 */
	public InetAddress getAddress() {
		return address;
	}
	
	/**
	 * Get the port number of the connection.
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

}
