import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class VirtualMachine {
	
	private static final int HOST_PORT = 5050;
	
	private final DatagramSocket client;
	private final InetAddress relay;
	private final String vmname;
	
	/**
	 * Create a new virtual machine
	 * @param client the socket that will send and recv data
	 * @param relay the name of host on which it's running
	 * @param hostname the name of the vm
	 * @throws UnknownHostException 
	 */
	public VirtualMachine(DatagramSocket client, String relay,
			String vmname) throws UnknownHostException {
		super();
		this.client = client;
		this.relay = InetAddress.getByName(relay);
		this.vmname = vmname;
	}
	
	/**
	 * Create a new virtual machine
	 * @param client the socket that will send and recv data
	 * @param relay the host on which it's running
	 * @param hostname the name of the vm
	 * @throws UnknownHostException 
	 */
	public VirtualMachine(DatagramSocket client, Host relay, String vmname){
		super();
		this.client = client;
		this.relay = relay.getIp();
		this.vmname = vmname;
	}
	
	/**
	 * @return the client
	 */
	public DatagramSocket getClient() {
		return client;
	}

	/**
	 * @return the relay
	 */
	public InetAddress getRelay() {
		return relay;
	}

	/**
	 * @return the vmname
	 */
	public String getVmname() {
		return vmname;
	}

	/**
	 * Clear the vm cache
	 */
	public void clearCache(){
		String msg = "relay:"+this.vmname+":clear";
		byte[] sendBytes = msg.getBytes();
		DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length, relay, HOST_PORT);
		try {
			client.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Send a message to the vm
	 * @param msg the message to send
	 */
	public void sendMessage(String msg){
		String data = "relay:"+this.vmname+":"+msg;
		byte[] sendBytes = data.getBytes();
		DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length, relay, HOST_PORT);
		try {
			client.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Start the fio process on the vm
	 * @param jobFile the job file for fio
	 */
	public void startFio(String jobFile){
		String msg = "relay:"+this.vmname+":fio:"+jobFile;
		byte[] sendBytes = msg.getBytes();
		DatagramPacket packet = new DatagramPacket(sendBytes, sendBytes.length, relay, HOST_PORT);
		try {
			client.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
