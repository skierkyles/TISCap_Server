/**
 * Original author - Greg Gagne.
 * 
 * Server to run multiple chat clients
 * 
 * @authors Kyle Swanson, Nicole Thomas
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class  ChatServer
{
	public static final int DEFAULT_PORT = 4020;
	
    // construct a thread pool for concurrency	
	private static final Executor exec = Executors.newCachedThreadPool();
	
	public static void main(String[] args) throws IOException {
		List<Connection> threads = Collections.synchronizedList(new ArrayList<Connection>());
		List<String> users = Collections.synchronizedList(new ArrayList<String>());
		
		ServerSocket sock = null;
		
		try {
			// establish the socket
			sock = new ServerSocket(DEFAULT_PORT);

			while (true) {
				// now listen for connections and service the connection in a separate thread.
				Connection c = new Connection(sock.accept(), threads, users);
				Runnable task = c;
				threads.add(c);
				exec.execute(task);
			}
		}
		catch (IOException ioe) { }
		finally {
			if (sock != null)
				sock.close();
		}
	}
}








