/**
 * Original author Greg Gagne 
 * 
 * A runnable connection object to help with spawning of additional threads. 
 * 
 * @author Kyle Swanson
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.Socket;
import java.util.Set;

public class Connection implements Runnable {
	public Socket client;
	public Set<Connection> clients;
	public OutputStream toClient;
	public InputStream fromClient;
	public static final int BUFFER_SIZE = 2048;

	// Some details about the current client.
	public String uname = "";
	public boolean active = false;

	public Connection(Socket client, Set<Connection> clients) {
		this.clients = clients;
		this.client = client;
	}

	/**
	 * This method runs in a separate thread.
	 */
	public void run() {
		// LOGIC UP IN HERE!
		byte[] buffer = new byte[BUFFER_SIZE];
		fromClient = null;
		toClient = null;

		try {
			/**
			 * get the input and output streams associated with the socket.
			 */
			fromClient = new BufferedInputStream(client.getInputStream());
			toClient = new BufferedOutputStream(client.getOutputStream());
			int numBytes;
			String host;

			/** continually loop until the client closes the connection */
			while ((numBytes = fromClient.read(buffer)) != -1) {
				// What the client sent us.
				host = new String(buffer, 0, numBytes);
				BufferedReader bufReader = new BufferedReader(new StringReader(
						host));
				String input = bufReader.readLine();
				ClientCommand cc = ClientCommand.parse(input);

				//Log the user in.
				while (active == false) {
					System.out.println(cc.command);
					
					if (!cc.command.equals("login")) {
						return;
					} else {
						uname = cc.arg;
						active = true;
					}
				}

				if (cc.command.equals("public")) {
					System.out.println("public msg: " + cc.data);
					publicToAllClients(cc.data);
				}
				// Somewhere in here we need a break;
			}
		} catch (IOException ioe) {
			System.err.println(ioe);
		} finally {
			// close streams and socket
			try {
				if (fromClient != null)
					fromClient.close();
				if (toClient != null)
					toClient.close();
			} catch (Exception e) {
			}
		}
	}
	
	public void publicToAllClients(String input) {
		for (Connection r : clients) {
			r.writePublicMessage(input);
		}
	}
	
	public void writePublicMessage(String msg) {
		writeToClient("Public " + uname + "\r\n" + msg);
	}

	public void writeToClient(String msg) {
		msg = "]" + msg;
		try {
			toClient.write(msg.getBytes());
			toClient.flush();
		} catch (IOException e) {
		}
	}

}
