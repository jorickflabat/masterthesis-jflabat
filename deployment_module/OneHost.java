import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.opennebula.client.ClientConfigurationException;


public class OneHost extends Host {
	

	private final int hostId;
	
	/**
	 * Create a new OpenNebula host
	 * @param client the socket that will send and recv data
	 * @param hostname the name of the host
	 * @param ip the ip address of the host
	 * @param hostId id of the host in OpenNebula
	 * @throws ClientConfigurationException 
	 */
	public OneHost(DatagramSocket client, String hostname, InetAddress ip, int hostId) throws ClientConfigurationException{
		super(client, hostname, ip);
		this.hostId = hostId;
	}
	
	/**
	 * Create a new OpenNebula host where the ip is retrieved from hostname
	 * @param client the socket that will send and recv data
	 * @param hostname the name of the host
	 * @param hostId id of the host in OpenNebula
	 * @throws UnknownHostException
	 * @throws ClientConfigurationException 
	 */
	public OneHost(DatagramSocket client, String hostname, int hostId) throws UnknownHostException, ClientConfigurationException{
		this(client, hostname, InetAddress.getByName(hostname), hostId);
	}
	
	/**
	 * @return the hostId
	 */
	public int getHostId(){
		return hostId;
	}
}
