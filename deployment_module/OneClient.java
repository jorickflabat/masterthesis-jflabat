import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;
import org.opennebula.client.OneResponse;
import org.opennebula.client.host.Host;
import org.opennebula.client.vm.VirtualMachine;



public class OneClient {
	
	private Client oneClient;
	
	public OneClient() throws ClientConfigurationException{
		this("oneadmin:oneadmin", "http://andromeda:2633/RPC2");
	}
	
	public OneClient(String credentials, String frontend) throws ClientConfigurationException{
		this.oneClient = new Client(credentials, frontend);
	}
	
	/**
	 * Instantiate a Virtual Machine on OpenNebula
	 * @param templateId the id of the template from which the vm is taken
	 * @param host the OpenNebula host on which it must be deployed
	 * @return the id of the vm
	 * @throws Exception
	 */
	public int instantiateVM(int templateId, OneHost host) throws Exception{
		return this.instantiateVM(templateId, host.getHostId());
	}
	
	/**
	 * Instantiate a Virtual Machine on OpenNebula
	 * @param templateId the id of the template from which the vm is taken
	 * @param hostId the id of the host on which it must be deployed
	 * @return the id of the vm
	 * @throws Exception
	 */
	public int instantiateVM(int templateId, int hostId) throws Exception{
		try {
			org.opennebula.client.template.Template temp = new org.opennebula.client.template.Template(templateId, oneClient);
			OneResponse res = temp.instantiate();
			
			if(res.isError()){
				System.err.println("Failed to instantiate the VM from template "+templateId);
				throw new Exception(res.getErrorMessage());
			}
			
			int id = Integer.parseInt(res.getMessage());
			VirtualMachine vm = new VirtualMachine(id, oneClient);
			res = vm.deploy(hostId);
			if(res.isError()){
				System.err.println("Failed to deploy a VM "+id);
				throw new Exception(res.getErrorMessage());
			}
			
			System.out.println("VM from template "+templateId+ " has been correctly deployed on "+hostId);
			
			return id;
			
		} catch (ClientConfigurationException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	/**
	 * Instantiate a Virtual Machine on OpenNebula
	 * @param templateId the id of the template from which the vm is taken
	 * @return the id of the vm
	 * @throws Exception
	 */
	public int instantiateVM(int templateId) throws Exception{
		try {
			org.opennebula.client.template.Template temp = new org.opennebula.client.template.Template(templateId, oneClient);
			OneResponse res = temp.instantiate();
			
			if(res.isError()){
				System.err.println("Failed to instantiate the VM from template "+templateId);
				throw new Exception(res.getErrorMessage());
			}
			
			int id = Integer.parseInt(res.getMessage());
			
			return id;
			
		} catch (ClientConfigurationException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	/**
	 * Retrieve vm informations
	 * @param vmId the identifier of the virtual machine
	 * @return the information in XML format
	 * @throws Exception
	 */
	public String getVMInformations(int vmId) throws Exception{
		VirtualMachine vm = new VirtualMachine(vmId, oneClient);
		OneResponse res = vm.info();
		
		if(res.isError()){
			System.err.println("Failed to retieve informations VM "+vmId);
			throw new Exception(res.getErrorMessage());
		}
		
		return res.getMessage();
	}
	
	/**
	 * Retrieve host informations
	 * @param hostId the identifier of the host
	 * @return the information in XML format
	 * @throws Exception
	 */
	public String getHostInformations(int hostId) throws Exception{
		Host host = new Host(hostId, oneClient);
		OneResponse res = host.info();
		
		if(res.isError()){
			System.err.println("Failed to retrieve informations Host "+hostId);
			throw new Exception(res.getErrorMessage());
		}
		
		return res.getMessage();
	}
	
	/**
	 * Shutdown a specific VM
	 * @param vmId the id of the vm
	 * @throws Exception 
	 */
	public void shutdownVM(int vmId) throws Exception{
		try {
			VirtualMachine vm = new VirtualMachine(vmId, oneClient);
			OneResponse res = vm.shutdown();
			
			if(res.isError()){
				System.err.println("Failed to shutdown VM "+vmId);
				throw new Exception(res.getErrorMessage());
			}
			
			System.out.println("VM "+vmId+" is shutting down...");
		} catch (ClientConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create a vm from a given template
	 * @param template the template of the VM
	 * @return the vm id
	 * @throws Exception 
	 */
	public int create(Template template) throws Exception{
		OneResponse res = VirtualMachine.allocate(oneClient, Template.toEqualFormat(template.getTemplateXML()));
		
		if(res.isError()){
			System.err.println("Cannot allocate vm...");
			System.err.println(Template.toEqualFormat(template.getTemplateXML()));
			throw new Exception(res.getErrorMessage());
		}
		
		return Integer.parseInt(res.getMessage());
	}
	
	/**
	 * Deploy a vm instance on hid
	 * @param vid the id of the vm instance
	 * @param hid the id of the host where to deploy
	 * @throws Exception 
	 */
	public void deploy(int vid, int hid) throws Exception{
		VirtualMachine vm = new VirtualMachine(vid, oneClient);
		OneResponse res = vm.deploy(hid);
		
		if(res.isError()){
			System.err.println("Cannot deploy vm "+vid+" on "+hid);
			throw new Exception(res.getErrorMessage());
		}
	}
	
	/**
	 * Hold a virtual machine instance
	 * @param vid the id of the VM to delete
	 * @throws Exception 
	 */
	public void hold(int vid) throws Exception{
		VirtualMachine vm = new VirtualMachine(vid, oneClient);
		OneResponse res = vm.hold();
		if(res.isError()){
			System.err.println("Fails to hold vm "+vid);
			throw new Exception(res.getErrorMessage());
		}
		
	}
	
	/**
	 * Get a registered template
	 * @param templateId the id of the template
	 * @return the template in XML format
	 * @throws Exception
	 */
	public Template getTemplate(int templateId) throws Exception{
		OneResponse res = org.opennebula.client.template.Template.info(oneClient, templateId);
		
		if(res.isError()){
			System.err.println("Error while retrieving template "+templateId);
			throw new Exception(res.getErrorMessage());
		}
		
		return new Template(res.getMessage());
	}
	
	/**
	 * Get the template of a VM
	 * @param vmId the id of the vm
	 * @return the template in XML format
	 * @throws Exception 
	 */
	public Template getVMTemplate(int vmId) throws Exception{
		String info = getVMInformations(vmId);
		int start = info.indexOf("<TEMPLATE>");
		int end = info.indexOf("</TEMPLATE>");
		if(start == -1 || end == -1)
			throw new Exception("No template for that VM");
		return new Template(info.substring(start, end+11));
	}
	
	public static void main(String[] args){
		OneClient one;
		try {
			one = new OneClient();
			Template temp = one.getVMTemplate(256);
			System.out.println(Template.removeNetworkStuff(temp.getTemplateXML()));
		} catch (ClientConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
