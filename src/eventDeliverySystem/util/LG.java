package eventDeliverySystem.util;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * A logger that prints output and debug messages to an output stream.
 *
 * @author Alex Mandelias
 */
public class LG {

	private static int tab = 0;

	public static void sout(String format, Object... args) {
		System.out.printf("\t".repeat(LG.tab) + format + "\n", args);
		System.out.flush();
	}

	public static void in() {
		LG.tab++;
	}

	public static void out() {
		LG.tab--;
	}

	public static void tab(String format, Object... args) {
		LG.in();
		LG.sout(format, args);
		LG.out();
	}

	public static void ttab(String format, Object... args) {
		LG.in();
		LG.in();
		LG.sout(format, args);
		LG.out();
		LG.out();
	}

	public static void tttab(String format, Object... args) {
		LG.in();
		LG.in();
		LG.in();
		LG.sout(format, args);
		LG.out();
		LG.out();
		LG.out();
	}

	public static void args(String... args) {
		LG.sout("Arg count: %d", args.length);
		for (int i = 0; i < args.length; i++)
			LG.sout("Arg %5d: %s", i, args[i]);
	}

	public static void ssocket(String prompt, Socket socket) {
		LG.sout("%s:%n\t%s", prompt, socket);
	}

	public static void ssocket(String prompt, ServerSocket serverSocket) {
		LG.sout("%s:%n%s", prompt, serverSocket);
	}

	public static void socket(String type, ServerSocket serverSocket) {
		LG.sout("%s IP   - %s%n%s Port - %d", type, serverSocket.getInetAddress(), type,
		        serverSocket.getLocalPort());
	}

	public static void socket(String type, Socket socket) {
		LG.sout("%s IP   - %s%n%s Port - %d", type, socket.getInetAddress(), type,
		        socket.getLocalPort());
	}
}
