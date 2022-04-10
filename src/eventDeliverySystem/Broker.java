package eventDeliverySystem;

import java.net.ServerSocket;
import java.net.Socket;

class Broker implements Runnable {
	public static final int SERVER_PORT = 49672; 
	
	private Socket toDefaultSocket;
	private ServerSocket fromDefaultSocket;
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
