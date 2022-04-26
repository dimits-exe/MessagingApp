package eventDeliverySystem;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * TODO
 *
 *
 * @author Alex Mandelias
 */
public class LG {

	public static void sout(String format, Object... args) {
		System.out.printf(format + "\n", args);
		System.out.flush();
	}

	public static void args(String... args) {
		sout("Arg count: %d", args.length);
		for (int i = 0; i < args.length; i++) {
			sout("Arg %5d: %s", i, args[i]);
		}
	}

	public static void ssocket(String prompt, Socket socket) {
		sout("%s:%n\t%s", prompt, socket);
	}

	public static void ssocket(String prompt, ServerSocket serverSocket) {
		sout("%s:%n%s", prompt, serverSocket);
	}

	public static void socket(String type, ServerSocket serverSocket) {
		sout("%s IP   - %s%n%s Port - %d", type, serverSocket.getInetAddress(), type,
		        serverSocket.getLocalPort());
	}

	public static void socket(String type, Socket socket) {
		sout("%s IP   - %s%n%s Port - %d", type, socket.getInetAddress(), type,
		        socket.getLocalPort());
	}
}
