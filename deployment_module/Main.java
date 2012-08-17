import java.net.DatagramSocket;

public class Main {

	public static void main(String[] args) throws Exception{
		
		DatagramSocket socket = new DatagramSocket();
		
		OneHost gemini = new OneHost(socket, "gemini", 6);
		OneHost hercules = new OneHost(socket, "hercules", 5);
		OneHost bootes = new OneHost(socket, "bootes", 3);
		Host frontend = new Host(socket, "andromeda");
		
		OneClient one = new OneClient();
		
		bootes.clearCache();
		gemini.clearCache();
		hercules.clearCache();
		frontend.clearCache();
		
		bootes.probe();
		gemini.probe();
		hercules.probe();
		
		one.instantiateVM(2, bootes);
				
		hercules.httpRequest("192.168.122.59");
		bootes.httpRequest("192.168.122.59");
		gemini.httpRequest("192.168.122.59");
	}
}
