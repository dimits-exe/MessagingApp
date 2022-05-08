package eventDeliverySystem.datastructures;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * An immutable pair of <IPAddress, Port> representing a unique connection in
 * the web.
 *
 * @author Dimitris Tsirmpas
 */
public final class ConnectionInfo implements Serializable {

	private static final long serialVersionUID = -3742145900403155967L;

	private final InetAddress address;
	private final int         port;

	/**
	 * Create a new ConnectionInfo representing the given server socket connection.
	 *
	 * @param connection the server socket whose details will be used.
	 */
	public ConnectionInfo(ServerSocket connection) {
		this(connection.getInetAddress(), connection.getLocalPort());
	}

	/**
	 * Create a new ConnectionInfo representing the given socket connection.
	 *
	 * @param connection the socket whose details will be used.
	 */
	public ConnectionInfo(Socket connection) {
		this(connection.getInetAddress(), connection.getLocalPort());
	}

	/**
	 * Construct a new info object with the connection details of the host.
	 *
	 * @param address the IP address of the host
	 * @param port    the port number of the host
	 */
	public ConnectionInfo(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}

	/**
	 * Get the IP address of the connection.
	 *
	 * @return the address
	 */
	public InetAddress getAddress() {
		return address;
	}

	/**
	 * Get the port number of the connection.
	 *
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	@Override
	public String toString() {
		return String.format("ConnectionInfo [address=%s, port=%d]", address, port);
	}
}
