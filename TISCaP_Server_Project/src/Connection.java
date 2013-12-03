/**
 * 
 * A runnable connection object to help with spawning of additional threads.
 * 
 * Handles syntax and other errors from client.
 * 
 * @authors Kyle Swanson, Nicole Thomas
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

				System.out.println("Requested Command: " + cc.command);

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
					System.out.println("public msg:" + cc.data);
					publicToAllClients(cc.data, uname);
				} else if (cc.command.equals("private")) {
					if (cc.arg.isEmpty()) {
						writeError("Enter a username.");
					} else {
						// TODO: data not getting set...
						System.out.println("data = " + cc.data);
						System.out.println("arg = " + cc.arg);
						privateToUser(cc.data, cc.arg);
					}
				} else if (cc.command.equals("users")) {
					// check for extra args: bad syntax
					if (cc.arg.equals("")) {
						writeUserList(users);
					} else {
						writeBadSyntax("/activeusers takes no arguments");
					}
				} else if (cc.command.equals("close")) {
					// check for extra args: bad syntax
					if (cc.arg.equals("")) {
						writeToAllClients("Disconnected " + uname + "\r\n");
						client.close();
						users.remove(uname);
						clients.remove(this);
					} else {
						writeBadSyntax("/close takes no arguments");
					}
				} else if (cc.command.equals("login")) {
					// if the user logs in, but hasn't submitted a new command,
					// do not throw an error
					continue;
				} else {
					// if the user enters else, it's not a supported command!
					writeBadSyntax("");
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
				writeError("User not found");
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
			toClient.write(msg.getBytes("UTF-8"));
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

	// public void run() {
	// byte[] buffer = new byte[BUFFER_SIZE];
	// fromClient = null;
	// toClient = null;
	//
	// try {
	// /**
	// * get the input and output streams associated with the socket.
	// */
	// fromClient = new BufferedInputStream(client.getInputStream());
	// toClient = new BufferedOutputStream(client.getOutputStream());
	// int numBytes;
	// String host;
	//
	// /** continually loop until the client closes the connection */
	// while ((numBytes = fromClient.read(buffer)) != -1) {
	// // What the client sent us.
	// host = new String(buffer, 0, numBytes);
	// BufferedReader bufReader = new BufferedReader(new StringReader(host));
	//
	// // This gets the entire message including line breaks.
	// char[] cin = new char[2048];
	// bufReader.read(cin);
	//
	// String input = String.valueOf(cin);
	// input = input.trim();
	//
	// ClientCommand cc = ClientCommand.parse(input);
	//
	// // check if user has already logged in
	// if (active == true && cc.command.equals("login")) {
	// writeError("You have already logged in.");
	// }
	//
	// // Log the user in.
	// while (active == false) {
	// if (!cc.command.equals("login") || cc.arg.isEmpty() ||
	// cc.command.equals("")) {
	// writeBadSyntax("Login command is '/login username \\r\\n\'");
	// return;
	// } else {
	// if (users.contains(cc.arg)) {
	// writeToClient("UsernameTaken\r\n");
	// // Close the connection.
	// client.close();
	// } else if (cc.arg.length() > 16) {
	// writeError("Username must be between 1 and 16 characters");
	// // Close the connection.
	// client.close();
	// } else {
	// uname = cc.arg;
	// users.add(uname);
	// active = true;
	//
	// writeToClient("Welcome\r\n");
	// writeToAllClients("Connected " + uname + "\r\n");
	// }
	// }
	// }
	//
	// // command and error handling
	// if (cc.command.equals("public")) {
	// // public messages
	// if (!cc.arg.equals("\\r\\n")) {
	// writeBadSyntax("");
	// } else {
	// publicToAllClients(cc.data, uname);
	// }
	// } else if (cc.command.equals("private")) {
	// // private messages
	// String args[] = cc.arg.split(" ");
	// if (args.length > 1) {
	// String name = args[0];
	// String rtn = args[1];
	// if (name.equals("\\r\\n")) {
	// writeError("Enter a username.");
	// } else if (!rtn.equals("\\r\\n")) {
	// writeBadSyntax("Private command is '/private username \\r\\n mesage\'");
	// }
	// } else {
	// privateToUser(cc.data, cc.arg);
	// }
	// } else if (cc.command.equals("users")) {
	// // users: check for extra arguments
	// if (cc.arg.equals("")) {
	// writeUserList(users);
	// } else {
	// writeBadSyntax("/users takes no arguments");
	// }
	// } else if (cc.command.equals("close")) {
	// // close: check for extra arguments
	// if (cc.arg.equals("")) {
	// writeToAllClients("Disconnected " + uname + "\r\n");
	// client.close();
	// users.remove(uname);
	// clients.remove(this);
	// } else {
	// writeBadSyntax("/close takes no arguments");
	// }
	// } else if (cc.command.equals("login")) {
	// // handles lack of command immediately after logging in
	// continue;
	// } else {
	// // if user enters ANYTHING else, it's not a valid command!
	// writeBadSyntax("");
	// }
	// }
	// } catch (IOException ioe) {
	// System.err.println(ioe);
	// } finally {
	// // close streams and socket
	// try {
	// if (fromClient != null)
	// fromClient.close();
	// if (toClient != null)
	// toClient.close();
	// } catch (Exception e) {}
	// }
	// }
	//
	// /**
	// * Helper methods!
	// */
	//
	// // writes the user list to client
	// private void writeUserList(List<String> u) {
	// String out = "ActiveUsers {";
	// synchronized (u) {
	// Iterator<String> i = u.iterator();
	// while (i.hasNext()) {
	// out = out + i.next();
	//
	// if (i.hasNext())
	// out = out + ",";
	// }
	// }
	// out = out + "}\r\n";
	// writeToClient(out);
	// }
	//
	// // writes a private message to a specified user
	// private void privateToUser(String input, String dest_uname) {
	// synchronized (clients) {
	// Connection dst = null;
	// for (Connection s : clients) {
	// if (s.uname.equals(dest_uname)) {
	// dst = s;
	// break;
	// }
	// }
	// if (dst != null && dest_uname.equals(uname)) {
	// // user is sending private message to self: allowed, but silly
	// String funny = "Why are you writing messages to yourself?\r\n";
	// dst.writeToClient("Private " + uname + "\r\n" + funny + input +
	// "\u0004\r\n");
	// } else if (dst != null) {
	// // main private message
	// dst.writeToClient("Private " + uname + "\r\n" + input + "\u0004\r\n");
	// } else {
	// // username was not found
	// writeError("User '" + dest_uname + "' not found");
	// }
	// }
	// }
	//
	// // writes input to all clients, such as connected and disconnected users
	// private void writeToAllClients(String input) {
	// synchronized (clients) {
	// Iterator<Connection> i = clients.iterator();
	// while (i.hasNext()) {
	// Connection r = i.next();
	// if (r.active)
	// r.writeToClient(input);
	// }
	// }
	// }
	//
	// // writes a public messages to all clients
	// private void publicToAllClients(String input, String sender_uname) {
	// synchronized (clients) {
	// Iterator<Connection> i = clients.iterator();
	// while (i.hasNext()) {
	// Connection r = i.next();
	// if (r.active)
	// r.writePublicMessage(input, sender_uname);
	// }
	// }
	// }
	//
	// // helper method for publicToAllClients
	// private void writePublicMessage(String msg, String sender_uname) {
	// writeToClient("Public " + sender_uname + "\r\n" + msg + "\u0004\r\n");
	// }
	//
	// // meat and potatoes - writing messages!
	// private void writeToClient(String msg) {
	// msg = "]" + msg;
	// try {
	// toClient.write(msg.getBytes("UTF-8"));
	// toClient.flush();
	// } catch (IOException e) {
	// }
	// }
	//
	// /**
	// * Error message methods!
	// * @param msg
	// */
	//
	// // bad syntax: takes an input message if desired
	// private void writeBadSyntax(String msg) {
	// writeToClient("BadSyntax " + msg + "\r\n");
	// }
	//
	// // general error: takes an input message if desired
	// private void writeError(String msg) {
	// writeToClient("Error " + msg + "\r\n");
	// }

}
