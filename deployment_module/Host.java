import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class Host {

	private static final int HOST_PORT = 5050;

	private final DatagramSocket client;
	private final String hostname;
	private final InetAddress ip;

	/**
	 * Create a new host
	 * @param client the socket that will send and recv data
	 * @param hostname the name of the host
	 * @param ip the ip address of the host
	 */
	public Host(DatagramSocket client, String hostname, InetAddress ip){
		this.client = client;
		this.hostname = hostname;
		this.ip = ip;
	}

	/**
	 * Create a new host where the ip is retrieved from hostname
	 * @param client the socket that will send and recv data
	 * @param hostname the name of the host
	 * @throws UnknownHostException
	 */
	public Host(DatagramSocket client, String hostname) throws UnknownHostException{
		this(client, hostname, InetAddress.getByName(hostname));
	}
	
	/**
	 * @return the client
	 */
	public DatagramSocket getClient() {
		return client;
	}

	/**
	 * @return the hostname
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * @return the ip
	 */
	public InetAddress getIp() {
		return ip;
	}

	/**
	 * Clear the cache of the host
	 */
	public void clearCache(){
		String data = "clear";
		byte[] sendData = data.getBytes();
		DatagramPacket packet = new DatagramPacket(sendData, sendData.length, ip, HOST_PORT);
		try {
			client.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send a message to the host
	 * @param msg the message to send
	 */
	public void sendMessage(String msg){
		byte[] sendData = msg.getBytes();
		DatagramPacket packet = new DatagramPacket(sendData, sendData.length, ip, HOST_PORT);
		try {
			client.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Measure the time by an http request for a url
	 * @param url the url where to send the request
	 */
	public void httpRequest(String url){
		try {
			ResponseSocket resSock = new ResponseSocket(hostname);
			new Thread(resSock).start();
			String data = "httpprobe:"+url+":"+resSock.sock.getLocalPort();
			byte[] sendData = data.getBytes();
			DatagramPacket packet = new DatagramPacket(sendData, sendData.length, ip, HOST_PORT);
			try {
				client.send(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (SocketException e1) {
			e1.printStackTrace();
			return;
		}
	}

	/**
	 * Probe the host to get usage informations
	 */
	public void probe(){
		String msg = "probe";
		byte[] sendData = msg.getBytes();
		DatagramPacket packet = new DatagramPacket(sendData, sendData.length, ip, HOST_PORT);
		try {
			client.send(packet);
			byte[] recvData = new byte[1024];
			DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
			client.receive(recvPacket);
			String[] tokens = new String(recvPacket.getData()).split(":");
			System.out.println("Hostname: "+hostname);
			System.out.println("   Read: "+tokens[1]+" KB/s");
			System.out.println("   Write: "+tokens[2]+" KB/s");
			System.out.println("   Download: "+tokens[3]+" KB/s");
			System.out.println("   Upload: "+tokens[4]+" KB/s");
			System.out.println("   Freecpu: "+tokens[5]+" %");
			System.out.println("   Usedcpu: "+tokens[6]+" %");
			System.out.println("   Running VMs: "+tokens[7]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public double probeIORatio(){
		String msg = "probe";
		byte[] sendData = msg.getBytes();
		DatagramPacket packet = new DatagramPacket(sendData, sendData.length, ip, HOST_PORT);
		try {
			client.send(packet);
			byte[] recvData = new byte[1024];
			DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
			client.receive(recvPacket);
			String[] tokens = new String(recvPacket.getData()).split(":");
			return (Double.parseDouble(tokens[1])/1100.0+Double.parseDouble(tokens[2])/1100.0)/2;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	/**
	 * Return the read load of the host
	 * @return the read load of the host
	 */
	public int probeRead(){
		String msg = "probe";
		byte[] sendData = msg.getBytes();
		DatagramPacket packet = new DatagramPacket(sendData, sendData.length, ip, HOST_PORT);
		try {
			client.send(packet);
			byte[] recvData = new byte[1024];
			DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
			client.receive(recvPacket);
			String[] tokens = new String(recvPacket.getData()).split(":");
			return Integer.parseInt(tokens[1]);
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	/**
	 * Return the write load of the host
	 * @return the write load of the host
	 */
	public int probeWrite(){
		String msg = "probe";
		byte[] sendData = msg.getBytes();
		DatagramPacket packet = new DatagramPacket(sendData, sendData.length, ip, HOST_PORT);
		try {
			client.send(packet);
			byte[] recvData = new byte[1024];
			DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
			client.receive(recvPacket);
			String[] tokens = new String(recvPacket.getData()).split(":");
			return Integer.parseInt(tokens[2]);
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	/**
	 * Ping the remote host
	 * @return true if host reachable, else false
	 */
	public boolean ping(){
		String msg = "ping";
		byte[] sendData = msg.getBytes();
		DatagramPacket packet = new DatagramPacket(sendData, sendData.length, ip, HOST_PORT);
		try {
			client.send(packet);
			byte[] recvData = new byte[1024];
			DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
			client.receive(recvPacket);
			System.out.println("Pong received from "+hostname);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}

/**
 * @author jflabat
 * Class that implements a socket that is waiting for a response
 */
class ResponseSocket implements Runnable{

	DatagramSocket sock;
	String hostname;
	
	public ResponseSocket(String hostname) throws SocketException{
		sock = new DatagramSocket();
		this.hostname = hostname;
	}
	
	@Override
	public void run() {
		byte[] recvData = new byte[1024];
		DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
		try {
			sock.receive(recvPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(hostname+": "+new String(recvPacket.getData()));
		sock.close();
	}
	
}
