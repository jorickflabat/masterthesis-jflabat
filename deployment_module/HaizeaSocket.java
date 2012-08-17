import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.opennebula.client.ClientConfigurationException;


public class HaizeaSocket implements Runnable{

	private Socket client;
	private OneClient one;
	private HashMap<String, int[]> storages;
	
	private DatagramSocket socket;
	private OneHost gemini;
	private OneHost hercules;
	private OneHost bootes;

	public HaizeaSocket(Socket client) throws ClientConfigurationException, SocketException, UnknownHostException{
		this.client = client;
		this.one = new OneClient();
		this.storages = new HashMap<String, int[]>();
		
		socket = new DatagramSocket();
		
		//Insert here the host with its id in OpenNebula
		gemini = new OneHost(socket, "gemini", 6);
		hercules = new OneHost(socket, "hercules", 5);
		bootes = new OneHost(socket, "bootes", 3);
		
		//Define each volume with the two hosts that contain the volume locally
		//the volumes are located in /data, if they are elsewhere, you should change
		//in makeImageCopy(String src, String name, String storage) of GlusterFSTools.java /data/ by 
		//what you want.
		int[] bohe = {3,5};
		int[] bege = {3,6};
		int[] gehe = {6,5};
		storages.put("bohe", bohe);
		storages.put("bege", bege);
		storages.put("gehe", gehe);
	}

	@Override
	public void run() {
		OutputStream output = null;
		InputStream input = null;
		byte[] buff = new byte[1024];
		try {
			output = client.getOutputStream();
			input = client.getInputStream();

			while(true){
				System.out.println("Wait for msg...");
				int size = input.read(buff);
				if(size<0)
					continue;
				String msg = new String(buff,0,size);
				System.out.println("Message received from "+client.getInetAddress()+" "+msg);
				StringTokenizer tokenizer = new StringTokenizer(msg, ":");

				if(tokenizer.countTokens() == 0)
					continue;

				if(tokenizer.nextToken().equals("DEPLOY")){
					if(tokenizer.countTokens() != 2){
						System.err.println("Error in message from haizea: "+msg);
						System.err.println("Only "+tokenizer.countTokens()+" but 2 expected");
					}
					int vid = Integer.parseInt(tokenizer.nextToken());
					int hid = Integer.parseInt(tokenizer.nextToken());
					System.out.println("Deployment of vm "+vid+" on "+hid);
					try {
						deployWithReplication(vid, hid);
						String success = "SUCCESS";
						output.write(success.getBytes());
						output.flush();
					} catch (Exception e) {
						e.printStackTrace();
						String error = "ERROR:"+e.getMessage();
						output.write(error.getBytes());
						output.flush();
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try{
				output.close();
				input.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * The vm vid must be deployed on host hid
	 * If the vid must be cloned and saved, a copy of the image
	 * is performed beforehand
	 * @param vid the id of the vm
	 * @param hid the id of the host
	 * @throws Exception 
	 */
	@SuppressWarnings("unused")
	private void deploy(int vid, int hid) throws Exception {
		Template template = one.getVMTemplate(vid);
		if(template.isCloned() && template.isSaved()){
			Template temp2 = template.makeCloneInGlusterFS(hid);
			int newVid = one.create(temp2);
			one.deploy(newVid, hid);
			one.hold(vid);
		}
		else{
			one.deploy(vid, hid);
		}

	}
	
	/**
	 * The vm vid must be deployed on host hid
	 * If the vid must be cloned and saved, a copy of the image
	 * is performed beforehand
	 * @param vid the id of the vm
	 * @param hid the id of the host
	 * @throws Exception 
	 */
	private void deployWithReplication(int vid, int hid) throws Exception {
		Template template = one.getVMTemplate(vid);
		if(template.isCloned() && template.isSaved()){
			double ratioLocal = getHost(hid).probeIORatio();
			double max = 0;
			String storageMax = null;
			for(StorageOtherHost soh:getLocalStorages(hid)){
				double ratioOther = getHost(soh.otherHost).probeIORatio();
				double ratio = (ratioLocal+ratioOther)/2.0;
				if(ratio > max){
					max = ratio;
					storageMax = soh.storage;
				}
			}
			Template temp2 = template.makeCloneInGlusterFS(storageMax);
			int newVid = one.create(temp2);
			one.deploy(newVid, hid);
			one.hold(vid);
		}
		else{
			one.deploy(vid, hid);
		}

	}
	
	/**
	 * Returns the corresponding to hid
	 * @param hid the id of the host
	 * @return the right host that has hid as id
	 */
	private OneHost getHost(int hid){
		switch(hid){
		case 3: return bootes;
		case 5: return hercules;
		case 6: return gemini;
		default: return null;
		}
	}
	
	/**
	 * Return a list of local storages for that host
	 * @param hid the id of the host
	 * @return the list of storages with the other host for that storage
	 */
	private List<StorageOtherHost> getLocalStorages(int hid){
		ArrayList<StorageOtherHost> list = new ArrayList<HaizeaSocket.StorageOtherHost>();
		for(String storage:this.storages.keySet()){
			if(storages.get(storage)[0]==hid)
				list.add(new StorageOtherHost(storage, storages.get(storage)[1]));
			else if(storages.get(storage)[1]==hid)
				list.add(new StorageOtherHost(storage, storages.get(storage)[0]));
			else
				continue;
		}
		return list;
	}
	
	/**
	 * Class representing a pair (storage,host)
	 * @author jflabat
	 *
	 */
	class StorageOtherHost {
		String storage;
		int otherHost;
		
		public StorageOtherHost(String storage, int otherHost) {
			this.storage = storage;
			this.otherHost = otherHost;
		}
	}
}
