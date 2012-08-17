import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.opennebula.client.ClientConfigurationException;


public class Probe {
	
	public static void main(String[] args) throws SocketException, UnknownHostException, ClientConfigurationException{
		DatagramSocket socket = new DatagramSocket();
		
		OneHost gemini = new OneHost(socket, "gemini", 6);
		OneHost hercules = new OneHost(socket, "hercules", 5);
		OneHost bootes = new OneHost(socket, "bootes", 3);
		//Host frontend = new Host(socket, "andromeda");
		
		bootes.probe();
		gemini.probe();
		hercules.probe();
	}
}
