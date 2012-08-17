import java.net.DatagramSocket;


public class CleanCaches {
	public static void main(String[] args) throws Exception{

		DatagramSocket socket = new DatagramSocket();

		OneHost gemini = new OneHost(socket, "gemini", 4);
		OneHost hercules = new OneHost(socket, "hercules", 5);
		OneHost bootes = new OneHost(socket, "bootes", 3);
		Host frontend = new Host(socket, "andromeda");

		bootes.clearCache();
		gemini.clearCache();
		hercules.clearCache();
		frontend.clearCache();
	}
}
