public class ClientCommand {
	public String command;
	public String arg = "";
	public String data = "";

	public static ClientCommand parse(String input) {
		ClientCommand out = new ClientCommand();
		if (input.length() == 0) {
			out.command = "";
		} else {

			int s = input.indexOf("\\r\\n");
			System.out.println("first s = "+s);

			if (s == -1) {
				s = input.length();
				System.out.println("s = "+s);
			}

			String before = input.substring(0, s);
			if (s < input.length()) {
				out.data = input.substring(s + 4);
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
