import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.opennebula.client.ClientConfigurationException;


public class HaizeaListenServer {
	
	private static int PORT = 5005;
	private static ServerSocket SOCKET;
	
	public static void serverLoop(){
		try {
			SOCKET = new ServerSocket(PORT);
			while(true){
				Socket client = SOCKET.accept();
				System.out.println("Connection from "+client.getInetAddress());
				HaizeaSocket sock = new HaizeaSocket(client);
				new Thread(sock).run();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClientConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		GlusterFSTools.init();
		serverLoop();
	}
}
