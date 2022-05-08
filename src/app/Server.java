package app;

import java.util.concurrent.ThreadLocalRandom;

import eventDeliverySystem.server.Broker;
import eventDeliverySystem.util.LG;

/**
 * Runs a Server which can be configured by command-line arguments.
 *
 * @author Dimitris Tsirmpas
 */
public class Server {

	private static final String usage = "Usage:\n"
	        + "\t   java app.Server\n"
	        + "\tor java app.Server <ip> <port>\n"
	        + "\n"
	        + "Arguments for servers after the first one:\n"
	        + "\t<ip>\t\tthe ip of the first server (run 'ipconfig' on the first server)\n"
	        + "\t<port>\t\tthe port the first server listens to (See 'Broker Port' in the first server's console)\n";

	private Server() {}

	/**
	 * Starts a new broker as a process on the local machine. If args are provided
	 * the broker will attempt to connect to the leader broker. If not, the broker
	 * is considered the leader broker. When starting the server subsystem the first
	 * broker MUST be the leader.
	 *
	 * @param args empty if the broker is the leader, the IP address and port of the
	 *             leader otherwise.
	 */
	public static void main(String[] args) {

		if ((args.length != 2) && (args.length != 0)) {
			System.out.print(Server.usage);
			return;
		}

		LG.sout("Broker#main start");
		LG.args(args);

		// argument processing
		String  ip       = "";
		int     port     = -1;
		boolean isLeader = true;

		if (args.length == 2) {
			ip = args[0];
			try {
				port = Integer.parseInt(args[1]);
			} catch (final NumberFormatException e) {
				System.err.printf("Invalid port: %s", args[1]);
				return;
			}
			isLeader = false;
		}

		// broker thread naming
		String brokerId;
		if (isLeader)
			brokerId = "Main";
		else
			brokerId = Integer.toString(ThreadLocalRandom.current().nextInt(1, 1000));

		// broker execution
		try (Broker broker = isLeader ? new Broker() : new Broker(ip, port)) { //java 8 forces me to do this
			final Thread thread = new Thread(broker, "Broker-" + brokerId);
			thread.start();
			LG.sout("Broker#main end");
			thread.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}
}
