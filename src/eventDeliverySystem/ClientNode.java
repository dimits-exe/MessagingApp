package eventDeliverySystem;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A superclass for all client-side Nodes that connect to and send / receive
 * data from Brokers.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
abstract class ClientNode implements AutoCloseable {

	/**
	 * The Client Node's {@link CIManager} that manages the information about this
	 * Node's connections to brokers
	 */
	protected final CIManager topicCIManager;

	/**
	 * Constructs a Client Node that will connect to a specific default broker.
	 *
	 * @param defaultServerIP   the IP of the default broker, interpreted as
	 *                          {@link InetAddress#getByName(String)}.
	 * @param defaultServerPort the port of the default broker
	 *
	 * @throws UnknownHostException if no IP address for the host could be found, or
	 *                              if a scope_id was specified for a global IPv6
	 *                              address while resolving the defaultServerIP.
	 * @throws IOException          if an I/O error occurs while initialising the
	 *                              Client Node
	 */
	public ClientNode(String defaultServerIP, int defaultServerPort) throws IOException {
		this(InetAddress.getByName(defaultServerIP), defaultServerPort);
	}

	/**
	 * Constructs a Client Node that will connect to a specific default broker.
	 *
	 * @param defaultServerIP   the IP of the default broker, interpreted as
	 *                          {@link InetAddress#getByAddress(byte[])}.
	 * @param defaultServerPort the port of the default broker
	 *
	 * @throws UnknownHostException if IP address is of illegal length
	 * @throws IOException          if an I/O error occurs while initialising the
	 *                              Client Node
	 */
	public ClientNode(byte[] defaultServerIP, int defaultServerPort) throws IOException {
		this(InetAddress.getByAddress(defaultServerIP), defaultServerPort);
	}

	/**
	 * Constructs a Client Node that will connect to a specific default broker.
	 *
	 * @param ip   the InetAddress of the default broker
	 * @param port the port of the default broker
	 *
	 * @throws IOException if an I/O error occurs while initialising the Client Node
	 */
	protected ClientNode(InetAddress ip, int port) throws IOException {
		topicCIManager = new CIManager(new ConnectionInfo(ip, port));
	}

	@Override
	public final void close() throws IOException {
		topicCIManager.close();
		closeImpl();
	}

	/**
	 * Allows each subclass to optionally specify how to close itself. The default
	 * implementation does nothing.
	 *
	 * @throws IOException if an IOException occurs while closing the resource
	 */
	protected void closeImpl() throws IOException {}
}
