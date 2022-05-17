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

	private static final String USAGE = "Usage:\n"
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
		LG.args(args);

		final Broker broker;
		final String brokerId;

		if (args.length == 0) {
			broker = new Broker();
			brokerId = "Main";

		} else if (args.length == 2) {
			String ip = args[0];
			int    port;
			try {
				port = Integer.parseInt(args[1]);
			} catch (final NumberFormatException e) {
				System.err.printf("Invalid port: %s", args[1]);
				return;
			}

			broker = new Broker(ip, port);

			brokerId = Integer.toString(ThreadLocalRandom.current().nextInt(1, 1000));

		} else {
			System.out.println(Server.USAGE);
			return;
		}

		try (broker) {
			final Thread thread = new Thread(broker, "Broker-" + brokerId);
			thread.run();
		}
	}
}
