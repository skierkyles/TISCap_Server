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
		boolean debug = false;

		if (debug) {
			System.out.println("Raw Input: " + input);
		}

		ClientCommand out = new ClientCommand();

		if (input.length() == 0) {
			out.command = "";
		} else {

			// int s = input.indexOf("\\r\\n");
			//
			// if (s == -1) {
			// s = input.length();
			// }
			//
			// String before = input.substring(0, s);
			// if (s < input.length()) {
			// out.data = input.substring(s + 4);
			// System.out.println("data = " + out.data);
			// }
			//
			// String[] args = before.split(" ");
			// out.command = args[0].substring(1).toLowerCase();
			//
			// if (args.length > 1) {
			// out.arg = args[1];
			// System.out.println("arg = " + out.arg);
			// }
			input = input.substring(1);
			String[] msg = input.split("\\r\\n", 2);
			String[] c_a = msg[0].split(" ");
			if (c_a.length == 2) {
				out.arg = c_a[1];
			}
			out.command = c_a[0].toLowerCase();
			System.out.println("out command = '" + out.command + "'");
			if (msg.length == 2) {
				out.data = msg[1];
			}
		}

		return out;
	}
}

// public class ClientCommand {
//
// // instance variables
// protected String command;
// protected String arg = "";
// protected String data = "";
//
// // parses client input
// protected static ClientCommand parse(String input) {
//
// ClientCommand out = new ClientCommand();
//
// // check for empy command
// if (input.length() == 0) {
// out.command = "";
// } else {
//
// // split up the input so we can work with it
// String[] args = input.split(" ");
//
// // first word should be command
// out.command = args[0].substring(1).toLowerCase();
//
// // handle commands with arguments or data
// if (args.length > 1) {
//
// out.arg = args[1];
//
// // public message
// if (out.command.equals("public") && args[1].equals("\\r\\n")) {
// // form the client message from remaining arguments
// for (int i = 2; i < args.length; i++) {
// out.data = out.data.concat(args[i] + " ");
// }
// // remove any trailing spaces
// out.data = out.data.trim();
// }
//
// // private message
// if (out.command.equals("private") && args.length > 2) {
// // pre-error checking
// if (!args[2].equals("\\r\\n")) {
// out.arg = args[1] + " " + args[2];
// } else {
// // form the client message from remaining arguments
// for (int i = 3; i < args.length; i++) {
// out.data = out.data.concat(args[i] + " ");
// }
// }
// // remove any trailing spaces
// out.data = out.data.trim();
// }
// }
// }
// return out;
// }
// }
