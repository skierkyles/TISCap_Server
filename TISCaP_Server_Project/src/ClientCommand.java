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

			String[] args = input.split(" ");

			out.command = args[0].substring(1).toLowerCase();

			if (args.length > 1) {
				out.arg = args[1];
				// public message
				if (args[1].equals("\\r\\n")) {
					for (int i = 2; i < args.length; i++) {
						out.data = out.data.concat(args[i] + " ");
					}
					out.data = out.data.trim();
				}
				// private message
				if (args.length > 2 && args[2].equals("\\r\\n")) {
					for (int i = 3; i < args.length; i++) {
						out.data = out.data.concat(args[i] + " ");
					}
					out.data = out.data.trim();
				}
			}
		}
		return out;
	}
}
