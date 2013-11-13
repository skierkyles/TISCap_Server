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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Connection implements Runnable {
	public Socket client;
	public Set<Connection> clients;
	public List<String> users;
	public OutputStream toClient;
	public InputStream fromClient;
	public static final int BUFFER_SIZE = 2048;

	// Some details about the current client.
	public String uname = "";
	public boolean active = false;

	public Connection(Socket client, Set<Connection> clients, List<String> users) {
		this.clients = clients;
		this.client = client;
		this.users = users;
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
				
				System.out.println("Requested Command: " + cc.command);

				// Log the user in.
				while (active == false) {

					if (!cc.command.equals("login")) {
						return;
					} else {
						// TODO: Some logic here for user already exists.

						uname = cc.arg;
						users.add(uname);
						active = true;
						
						writeWelcome();
					}
				}

				if (cc.command.equals("public")) {
					System.out.println("public msg: " + cc.data);
					publicToAllClients(cc.data, uname);
				}
				
				if (cc.command.equals("private")) {

				}

				if (cc.command.equals("users")) {
					writeUserList(users);
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

	public void writeWelcome() {
		writeToClient("welcome\r\n");
	}
	
	public void writeUserList(List<String> u) {
		String out = "ActiveUsers {";

		synchronized(u) {
			Iterator<String> i = u.iterator();
			while (i.hasNext()) {
				out = out+i.next();

				if (i.hasNext()) 
					out = out+",";
			}
		}
		
		out = out + "}\r\n";

		
		writeToClient(out);
	}
	
	public void publicToAllClients(String input, String sender_uname) {
		for (Connection r : clients) {
			if (r.active)
				r.writePublicMessage(input, sender_uname);
		}
	}

	public void writePublicMessage(String msg, String sender_uname) {
		writeToClient("Public " + sender_uname + "\r\n" + msg);
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
