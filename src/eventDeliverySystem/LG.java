package eventDeliverySystem;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * A logger that prints output and debug messages to an output stream.
 *
 *
 * @author Alex Mandelias
 */
public class LG {

	private static int tab = 0;

	public static void sout(String format, Object... args) {
		System.out.printf("\t".repeat(tab) + format + "\n", args);
		System.out.flush();
	}

	public static void in() {
		tab++;
	}

	public static void out() {
		tab--;
	}

	public static void tab(String format, Object... args) {
		in();
		sout(format, args);
		out();
	}

	public static void ttab(String format, Object... args) {
		in();
		in();
		sout(format, args);
		out();
		out();
	}

	public static void tttab(String format, Object... args) {
		in();
		in();
		in();
		sout(format, args);
		out();
		out();
		out();
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
