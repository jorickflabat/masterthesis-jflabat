import java.io.IOException;
import java.util.HashMap;


public class GlusterFSTools {
	
	private static boolean makeCopy = false;
	private static HashMap<Integer, String> VOLUMES;
	
	/**
	 * Make a copy of the image
	 * @param src the source of the image
	 * @param dst the destination of the image
	 * @return true if it's been done, else false
	 */
	public static boolean makeImageCopy(String src, String dst){
		System.out.println("Copy the image "+src+" to "+dst);
		if(!makeCopy)
			return true;
		String command = "cp "+src+" "+dst;
		try {
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
			return (process.exitValue() == 0)?true:false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Make a copy of the image
	 * @param src the source of the image
	 * @param name the name of the new image
	 * @param hid the hostId where one of it's volume will contain the new image
	 */
	public static String makeImageCopy(String src, String name, int hid) {
		if(!VOLUMES.containsKey(hid)){
			System.err.println("No volume for host "+hid);
			return null;
		}
		String dst = VOLUMES.get(hid) + name;
		if(makeImageCopy(src, dst))
			return dst;
		else
			return null;
	}
	
	/**
	 * Make a copy of the image
	 * @param src the source of the image
	 * @param name the name of the new image
	 * @param hid the hostId where one of it's volume will contain the new image
	 */
	public static String makeImageCopy(String src, String name, String storage) {
		String dst = "/data/" + storage + "/" + name;
		if(makeImageCopy(src, dst))
			return dst;
		else
			return null;
	}
	
	/**
	 * Initialize the gluster fs volumes
	 */
	public static void init(){
		VOLUMES = new HashMap<Integer, String>();
		VOLUMES.put(3, "/data/bege/");
		VOLUMES.put(6, "/data/gehe/");
		VOLUMES.put(5, "/data/bohe/");
	}

	
}
