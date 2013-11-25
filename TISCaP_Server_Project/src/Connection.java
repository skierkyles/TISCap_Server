/**
 * Original author Greg Gagne 
 * 
 * A runnable connection object to help with spawning of additional threads. 
 * 
 * @author Kyle Swanson & Nicole Thomas
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

public class Connection implements Runnable {
	public Socket client;
	public List<Connection> clients;
	public List<String> users;
	public OutputStream toClient;
	public InputStream fromClient;
	public static final int BUFFER_SIZE = 2048;

	// Some details about the current client.
	public String uname = "";
	public boolean active = false;

	public Connection(Socket client, List<Connection> clients, List<String> users) {
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
				BufferedReader bufReader = new BufferedReader(new StringReader(host));				

				// This gets the entire message including line breaks. 
				char[] cin = new char[2048];
				bufReader.read(cin);

				String input = String.valueOf(cin);
				input = input.trim();
				
				ClientCommand cc = ClientCommand.parse(input);

				System.out.println("Requested Command: " + cc.command);

				if (active == true && cc.command.equals("login")) {
					writeError("You have already logged in.");
				}

				// Log the user in.
				while (active == false) {
					if (!cc.command.equals("login") || cc.arg.isEmpty() || cc.command.equals("")) {
						writeBadSyntax("Login command is '/login username CRLF'");
						return;
					} else {
						if (users.contains(cc.arg)) {
							writeToClient("UsernameTaken\r\n");
							// Close the connection.
							client.close();
						} else {
							uname = cc.arg;
							users.add(uname);
							active = true;

							writeToClient("Welcome\r\n");
							writeToAllClients("Connected " + uname + "\r\n");
						}
					}
				}

				if (cc.command.equals("public")) {
					System.out.println("public msg:" + cc.data);
					publicToAllClients(cc.data, uname);
				}

				if (cc.command.equals("private")) {
					privateToUser(cc.data, cc.arg);
				}

				if (cc.command.equals("users")) {
					// check if extra args for bad syntax
					if (cc.arg.equals("")) {
						writeUserList(users);
					} else {
						writeBadSyntax("/activeusers takes no arguments");
					}
				}

				if (cc.command.equals("close")) {
					// check if extra args for bad syntax
					if (cc.arg.equals("")) {
						writeToAllClients("Disconnected " + uname + "\r\n");
						client.close();
						users.remove(uname);
						clients.remove(this);
					} else {
						writeBadSyntax("/close takes no arguments");
					}
				}
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

	private void writeUserList(List<String> u) {
		String out = "ActiveUsers {";

		synchronized (u) {
			Iterator<String> i = u.iterator();
			while (i.hasNext()) {
				out = out + i.next();

				if (i.hasNext())
					out = out + ",";
			}
		}

		out = out + "}\r\n";

		writeToClient(out);
	}

	private void privateToUser(String input, String dest_uname) {
		synchronized (clients) {
			Connection dst = null;
			for (Connection s : clients) {
				if (s.uname.equals(dest_uname)) {
					dst = s;
					break;
				}
			}

			if (dst != null) {
				dst.writeToClient("Private " + uname + "\r\n" + input + "\u0004");
			} else {
				// TODO Return UserNotFound
				System.out.println("User not found... '" + dest_uname + "'");
			}
		}
	}

	private void writeToAllClients(String input) {
		synchronized (clients) {
			Iterator<Connection> i = clients.iterator();
			while (i.hasNext()) {
				Connection r = i.next();
				if (r.active)
					r.writeToClient(input);
			}
		}
	}

	private void publicToAllClients(String input, String sender_uname) {
		synchronized (clients) {
			Iterator<Connection> i = clients.iterator();
			while (i.hasNext()) {
				Connection r = i.next();
				if (r.active)
					r.writePublicMessage(input, sender_uname);
			}
		}
	}

	private void writePublicMessage(String msg, String sender_uname) {
		writeToClient("Public " + sender_uname + "\r\n" + msg + "\u0004");
	}

	private void writeToClient(String msg) {
		msg = "]" + msg;
		try {
			toClient.write(msg.getBytes());
			toClient.flush();
		} catch (IOException e) {
		}
	}

	// errors

	private void writeBadSyntax(String msg) {
		writeToClient("BadSyntax " + msg + "\r\n");
	}

	private void writeError(String msg) {
		writeToClient("Error " + msg + "\r\n");
	}

}
