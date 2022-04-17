package eventDeliverySystem;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A class that manages and distributes port numbers to the various processes in
 * this machine. Used to allow more than one client/server process to run in one
 * machine by providing them with available ports.
 *
 * @author Dimitris Tsirmpas
 */
final class PortManager {

	private static final int LOWEST_PORT = 29170;
	private static final int HIGHEST_PORT = 29998;

	/**
	 * Returns a new port for this process in the range {@code 29170-29998} that is
	 * guaranteed to be available. This method is thread-safe.
	 *
	 * @return a new available port
	 */
	public synchronized int getNewAvailablePort() {
		int port;
		do {
			port = ThreadLocalRandom.current().nextInt(LOWEST_PORT, HIGHEST_PORT);
		} while (!PortManager.isAvailable(port));

		return port;
	}

	/**
	 * Checks whether a port is available.
	 *
	 * @param port the port to be checked
	 *
	 * @return {@code true} if the port is available, {@code false} otherwise
	 *
	 * @implNote the method attempts to create a connection through the port and
	 *           relies on exception handling to determine if it's already binded.
	 */
	// https://stackoverflow.com/questions/434718/sockets-discover-port-availability-using-java
	private static boolean isAvailable(int port) {
		try (ServerSocket testSocket = new ServerSocket(port)) {
	        testSocket.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
			return false;
	    }
	}
}
