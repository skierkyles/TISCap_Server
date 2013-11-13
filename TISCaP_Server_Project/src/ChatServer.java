/**
 * Original author - Greg Gagne.
 * 
 * 
 * @author Kyle Swanson
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class  ChatServer
{
	public static final int DEFAULT_PORT = 4020;
	// public static final String CONFIG = "/home/kyle/config.xml";
	
    // construct a thread pool for concurrency	
	private static final Executor exec = Executors.newCachedThreadPool();
	
	public static void main(String[] args) throws IOException {
		Set<Connection> threads = new HashSet<Connection>();
		
		ServerSocket sock = null;
		
		//System.out.println(cache.keySet().toString());
		
		try {
			// establish the socket
			sock = new ServerSocket(DEFAULT_PORT);

			while (true) {
				/**
				 * now listen for connections
				 * and service the connection in a separate thread.
				 */
				Connection c = new Connection(sock.accept(), threads);
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








