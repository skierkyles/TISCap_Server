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

			int s = input.indexOf("\r\n");

			if (s == -1) {
				s = input.length();
			}

			String before = input.substring(0, s);
			if (s < input.length()) {
				out.data = input.substring(s + 2);
			}

			String[] args = before.split(" ");
			out.command = args[0].substring(1).toLowerCase();

			if (args.length > 1) {
				out.arg = args[1];
			}
		}

		return out;
	}
}
