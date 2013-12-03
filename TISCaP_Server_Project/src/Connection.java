/**
 * 
 * A runnable connection object to help with spawning of additional threads.
 * 
 * Handles syntax and other errors from client.
 * 
 * @author Kyle Swanson, Nicole Thomas
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

	// static
	private static final int BUFFER_SIZE = 2048;

	// instance variables
	private Socket client;
	private List<Connection> clients;
	private List<String> users;
	private OutputStream toClient;
	private InputStream fromClient;

	// Some details about the current client.
	private String uname = "";
	private boolean active = false;

	public Connection(Socket client, List<Connection> clients, List<String> users) {
		this.clients = clients;
		this.client = client;
		this.users = users;
	}

	/**
	 * This method runs each user in a separate thread.
	 * 
	 * The only global method from this class.
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

				// check to see if user has already logged in 
				if (active == true && cc.command.equals("login")) {
					writeError("You have already logged in.");
				}

				// Log the user in.
				while (active == false) {
					if (!cc.command.equals("login") || cc.arg.isEmpty() || cc.command.equals("")) {
						writeBadSyntax("Login command is '/login username \\r\\n'");
						return;
					} else {
						if (users.contains(cc.arg)) {
							writeToClient("UsernameTaken\r\n");
							// Close the connection.
							client.close();
						} else if (cc.arg.length() > 16) {
							writeError("Username must be between 1 and 16 characters");
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

				// command and error handling
				if (cc.command.equals("public")) {
					// public messages
					if (cc.arg.isEmpty()) {
						publicToAllClients(cc.data, uname);
					} else {
						writeBadSyntax();
					}
				} else if (cc.command.equals("private")) {
					// private messages
					if (cc.arg.isEmpty()) {
						writeError("Enter a username.");
					} else {
						privateToUser(cc.data, cc.arg);
					}
				} else if (cc.command.equals("users")) {
					// users: check for extra arguments
					if (cc.arg.isEmpty()) {
						writeUserList(users);
					} else {
						writeBadSyntax("/activeusers takes no arguments");
					}
				} else if (cc.command.equals("close")) {
					// close: check for extra arguments
					if (cc.arg.isEmpty()) {
						writeToAllClients("Disconnected " + uname + "\r\n");
						client.close();
						users.remove(uname);
						clients.remove(this);
					} else {
						writeBadSyntax("/close takes no arguments");
					}
				} else if (cc.command.equals("login")) {
					// handles lack of command immediately after logging in
					continue;
				} else {
					// if user enters ANYTHING else, it's not a valid command!
					writeBadSyntax();
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

	/**
	 * Helper methods!
	 */

	// writes the user list to client
	private void writeUserList(List<String> u) {
		String out = "ActiveUsers ";
		synchronized (u) {
			Iterator<String> i = u.iterator();
			while (i.hasNext()) {
				out = out + i.next();
				if (i.hasNext())
					out = out + ",";
			}
		}
		out = out + "\r\n";
		writeToClient(out);
	}

	// writes a private message to a specified user
	private void privateToUser(String input, String dest_uname) {
		synchronized (clients) {
			Connection dst = null;
			for (Connection s : clients) {
				if (s.uname.equals(dest_uname)) {
					dst = s;
					break;
				}
			}
			if (dst != null && dest_uname.equals(uname)) {
				// user is sending private message to self: allowed, but silly
				String funny = "Why are you writing messages to yourself?\r\n";
				dst.writeToClient("Private " + uname + "\r\n" + funny + input + "\r\n");
			} else if (dst != null) {
				// main private message
				dst.writeToClient("Private " + uname + "\r\n" + input + "\r\n");
			} else {
				// username was not found
				writeError("User '" + dest_uname + "' not found");
			}
		}
	}

	// writes input to all clients, such as connected and disconnected users
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

	// writes a public messages to all clients
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

	// helper method for publicToAllClients
	private void writePublicMessage(String msg, String sender_uname) {
		writeToClient("Public " + sender_uname + "\r\n" + msg + "\r\n");
	}

	// meat and potatoes - writing messages!
	private void writeToClient(String msg) {
		msg = "]" + msg;
		try {
			// change bytes to utf-8 text
			toClient.write(msg.getBytes("UTF-8"));
			toClient.flush();
		} catch (IOException e) {
		}
	}

	/**
	 * Error message methods!
	 * 
	 * @param msg
	 */

	// bad syntax: no message
	private void writeBadSyntax() {
		writeToClient("BadSyntax\r\n");
	}

	// bad syntax: input message
	private void writeBadSyntax(String msg) {
		writeToClient("BadSyntax " + msg + "\r\n");
	}

	// general error: input message
	private void writeError(String msg) {
		writeToClient("Error " + msg + "\r\n");
	}

}
