/**
 * Handler for incoming client commands
 * 
 * Parses incoming strings and returns usable commands, arguments, and data
 * 
 * @authors Kyle Swanson, Nicole Thomas
 * 
 */

public class ClientCommand {
	public String command;
	public String arg = "";
	public String data = "";

	public static ClientCommand parse(String input) {

		ClientCommand out = new ClientCommand();

		if (input.length() == 0) {
			out.command = "";
		} else {
			input = input.substring(1); 				// remove first character '/'
			String[] msg = input.split("\\r\\n", 2); 	// split any incoming message into 2 parts, around the first \r\n
			String[] c_a = msg[0].split(" "); 			// handler for arguments after a command
			if (c_a.length == 2) { 						// if there is an argument, assign it
				out.arg = c_a[1];
			}
			out.command = c_a[0].toLowerCase();			// assign command, make "case-insensitive"
			if (msg.length == 2) {						// if there is data after the first \r\n, it's a public/private message
				out.data = msg[1];
			}
		}
		return out;
	}
}