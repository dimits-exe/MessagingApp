package eventDeliverySystem;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * A Thread for sending some data to an output stream.
 *
 * @author Alex Mandelias
 */
class PushThread extends Thread {

	private final Packet[]           data;
	private final ObjectOutputStream stream;
	private boolean                  success, start, end;

	/**
	 * Constructs the Thread that, when run, will write the data to the stream.
	 *
	 * @param data   the data to write
	 * @param stream the output stream to which to write the data
	 */
	public PushThread(Packet[] data, ObjectOutputStream stream) {
		this.data = data;
		this.stream = stream;
		success = start = end = false;
	}

	@Override
	public void run() {
		start = true;

		try {
			for (int i = 0; i < data.length; i++) {
				stream.writeObject(data[i]);
			}

			success = true;

		} catch (IOException e) {
			System.err.printf("IOException while sending packets to actual broker%n");
			success = false;
		}

		end = true;
	}

	/**
	 * Returns whether this Thread has executed its job successfully. This method
	 * shall be called after this Thread has executed its {@code run} method once.
	 *
	 * @return {@code true} if it has, {@code false} otherwise
	 *
	 * @throws IllegalStateException if this Thread has not completed its execution
	 *                               before this method is called
	 */
	public boolean success() throws IllegalStateException {
		if (!start)
			throw new IllegalStateException(
			        "Can't call 'success()' before starting this Thread");
		if (!end)
			throw new IllegalStateException(
			        "This Thread must finish execution before calling 'success()'");

		return success;
	}
}