package eventDeliverySystem;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A process that maintains connections to a designated broker, and
 * pushes new messages to the appropriate brokers.
 *
 */
class Publisher implements Runnable {
	
	private Socket clientSocket;
	private ServerSocket serverSocket;
	
	/**
	 * 
	 * @param defaultServerIP
	 * @throws IOException
	 */
	public Publisher(String defaultServerIP) throws IOException {
		//establish connection with default broker
		//read actual broker's IP
		//establish connection with new broker
		//wait for connection by the new broker
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
	public void push(String topic, byte[] data, DataType type) {
		Packet[] packets = Packet.dataToPackets(new RawData(data, type), topic);
		//send packets through clientSocket
	}
	
	private void changeBroker(InetAddress newIP) throws IOException {
		closeConnection();
		clientSocket = new Socket(newIP, 60);
	}
	
	private void closeConnection() throws IOException {
		clientSocket.close();
	}

	

}
