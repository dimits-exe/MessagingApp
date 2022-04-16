package eventDeliverySystem;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A class managing and distributing port numbers to the various processes
 * in the system.
 * Used to allow more than one client/server process to run in one machine.
 *
 */
final class PortManager {
	private static final int LOWEST_PORT = 29170;
	private static final int HIGHEST_PORT = 29998;
	
	/**
	 * Get a new port for the process. The port is guaranteed to be available.
	 * This method is thread-safe.
	 * 
	 * @return a new available port
	 */
	public synchronized int getPort() {
		int port;
		do {
			port = ThreadLocalRandom.current().nextInt(LOWEST_PORT, HIGHEST_PORT);
		} while(!isAvailable(port));
		
		return port;
	}
	
	/**
	 * Check whether the port is available.
	 * @param port the port to be checked
	 * @return true if the port is available, false otherwise
	 * 
	 * @implNote the method attempts to create a connection through the port
	 * and relies on exception handling to determine if it's already binded.
	 */
	private boolean isAvailable(int port) {
		ServerSocket testSocket = null;
	    try {
	        testSocket = new ServerSocket(port);
	        testSocket.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
	    } finally {
	        if (testSocket != null) {
	            try {
	                testSocket.close();
	            } catch (IOException e) {
	                /* should not be thrown */
	            }
	        }
	    }

	    return false;
	}
}
