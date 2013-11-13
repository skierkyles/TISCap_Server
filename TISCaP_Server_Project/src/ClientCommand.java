
public class ClientCommand {
	public String command;
	public String arg = "";
	public String data = "";
	
	public static ClientCommand parse(String input) {
		ClientCommand out = new ClientCommand();
		
		int s = input.indexOf("\\r\\n");
		if (s == -1) {
			s = input.length();
		}
		
		String before = input.substring(0, s);
		if (s < input.length()) {
			out.data = input.substring(s+4);
			//System.out.println("Here is my data: " + out.data);
		}
		
		String[] args = before.split(" ");
		out.command = args[0].substring(1);
		
		if (args.length > 1) {
			out.arg = args[1];
		}
		
		//System.out.println(out.command);
		//System.out.println(out.data);
		
		return out;
	}
}
