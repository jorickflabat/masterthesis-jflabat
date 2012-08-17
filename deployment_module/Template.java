import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class Template {

	private final String templateXML;
	private final boolean saved;
	private final boolean cloned;
	private final String imagePath;

	public Template(String templateXML){
		this.templateXML = templateXML;
		this.saved = isImageSaved(templateXML);
		this.cloned = isImageCloned(templateXML);
		this.imagePath = getImageSource(templateXML);
	}

	/**
	 * @return the saved
	 */
	public boolean isSaved() {
		return saved;
	}

	/**
	 * @return the cloned
	 */
	public boolean isCloned() {
		return cloned;
	}

	/**
	 * @return the imagePath
	 */
	public String getImagePath() {
		return imagePath;
	}

	/**
	 * @return the templateXML
	 */
	public String getTemplateXML() {
		return templateXML;
	}

	/**
	 * Make a clone in GlusterFS Volume
	 * @return the new vm template
	 */
	public Template makeCloneInGlusterFS(){
		int indexDot = indexOfLastDot(imagePath);
		String name = imagePath.substring(0, indexDot);
		String extension = imagePath.substring(indexDot);
		String dst = name+"_"+System.currentTimeMillis()+extension;
		GlusterFSTools.makeImageCopy(imagePath, dst);
		Template newTemp = new Template(removeNetworkStuff(changeCloneValue(changeSourceValue(changeSaveValue(templateXML), dst))));
		return newTemp;
	}

	/**
	 * Make a clone in GLusterFS volume owned by hid
	 * @param hid the id of the host
	 * @return the new vm template
	 */
	public Template makeCloneInGlusterFS(int hid){
		int indexDot = indexOfLastDot(imagePath);
		int indexSlash = indexOfLastSlash(imagePath);
		String name = imagePath.substring(indexSlash+1, indexDot)+"_"+System.currentTimeMillis();
		String extension = imagePath.substring(indexDot);
		String dst = "";
		dst = GlusterFSTools.makeImageCopy(imagePath, name+extension, hid);
		if(dst == null)
			return null;
		Template newTemp = new Template(removeNetworkStuff(changeCloneValue(changeSourceValue(changeSaveValue(templateXML), dst))));
		return newTemp;
	}
	
	/**
	 * Make a clone in GLusterFS volume storage
	 * @param hid the id of the host
	 * @return the new vm template
	 */
	public Template makeCloneInGlusterFS(String storage){
		int indexDot = indexOfLastDot(imagePath);
		int indexSlash = indexOfLastSlash(imagePath);
		String name = imagePath.substring(indexSlash+1, indexDot)+"_"+System.currentTimeMillis();
		String extension = imagePath.substring(indexDot);
		String dst = "";
		dst = GlusterFSTools.makeImageCopy(imagePath, name+extension, storage);
		if(dst == null)
			return null;
		Template newTemp = new Template(removeNetworkStuff(changeCloneValue(changeSourceValue(changeSaveValue(templateXML), dst))));
		return newTemp;
	}

	/**
	 * Check if the image in the template is saved
	 * @param templateXML the template in XML format
	 * @return true if it's saved, else false
	 */
	public static boolean isImageSaved(String templateXML){
		int start = templateXML.indexOf("<SAVE>");
		int end = templateXML.indexOf("</SAVE>");
		return (start!=1 && end!=-1 && templateXML.substring(start+6, end).contains("yes"))?true:false;
	}

	/**
	 * Check if the image in the template is cloned
	 * @param templateXML the template in XML format
	 * @return true if it's cloned, else false
	 */
	public static boolean isImageCloned(String templateXML){
		int start = templateXML.indexOf("<CLONE>");
		int end = templateXML.indexOf("</CLONE>");
		return (start!=1 && end!=-1 && templateXML.substring(start+7, end).contains("yes"))?true:false;
	}

	/**
	 * Retieve the image source from template
	 * @param templateXML the template in XML format
	 * @return the image path
	 */
	public static String getImageSource(String templateXML){
		int start = templateXML.indexOf("<SOURCE>");
		int end = templateXML.indexOf("</SOURCE>");
		return templateXML.substring(start+17, end-3);
	}

	/**
	 * Invert the save value in template
	 * @param templateXML the template in XML format
	 * @return the new template in XML format
	 */
	public static String changeSaveValue(String templateXML){
		int start = templateXML.indexOf("<SAVE>");
		int end = templateXML.indexOf("</SAVE>");
		if(start==-1 || end == -1)
			return templateXML;
		String left = templateXML.substring(0, start+15);
		String right = templateXML.substring(end-3);
		String newValue = (templateXML.substring(start+6, end).contains("yes"))?"no":"yes";
		return left+newValue+right;
	}

	/**
	 * Invert the clone value in template
	 * @param templateXML the template in XML format
	 * @return the new template in XML format
	 */
	public static String changeCloneValue(String templateXML){
		int start = templateXML.indexOf("<CLONE>");
		int end = templateXML.indexOf("</CLONE>");
		if(start==-1 || end == -1)
			return templateXML;
		String left = templateXML.substring(0, start+16);
		String right = templateXML.substring(end-3);
		String newValue = (templateXML.substring(start+7, end).contains("yes"))?"no":"yes";
		return left+newValue+right;
	}

	/**
	 * Change the source value in template
	 * @param templateXML the template in XML format
	 * @param newSource the new value to set
	 * @return the new template in XML format
	 */
	public static String changeSourceValue(String templateXML, String newSource){
		int start = templateXML.indexOf("<SOURCE>");
		int end = templateXML.indexOf("</SOURCE>");
		String left = templateXML.substring(0, start+17);
		String right = templateXML.substring(end-3);
		return left+newSource+right;
	}
	
	/**
	 * Remote network stuffs from the template
	 * @param templateXML the stuffs to remove
	 * @return the new template
	 */
	public static String removeNetworkStuff(String templateXML){
		int start = templateXML.indexOf("<BRIDGE>");
		int end = templateXML.indexOf("</NETWORK>");
		String left = templateXML.substring(0, start);
		String right = templateXML.substring(end+10);
		return left+right;
	}

	@Override
	public String toString(){
		return templateXML+"\nSaved: "+saved+"\nPath: "+imagePath+"\nCloned: "+cloned;
	}

	/**
	 * Get the index of the last dot in path
	 * @param path the string in which it searchs
	 * @return the last index of a dot in path
	 */
	private static int indexOfLastDot(String path){
		for(int i = path.length()-1; i>=0; i--){
			if(path.charAt(i)=='.')
				return i;
		}
		return -1;
	}

	/**
	 * Get the index of the last slash in path
	 * @param path the string in which it searchs
	 * @return the last index of a slash in path
	 */
	private static int indexOfLastSlash(String path){
		for(int i = path.length()-1; i>=0; i--){
			if(path.charAt(i)=='/')
				return i;
		}
		return -1;
	}

	public static String toEqualFormat(String templateXML){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		String theString = "";
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(templateXML)));
			Element root = doc.getDocumentElement();
			System.out.println("Root element: " + root.getNodeName());
			NodeList nList = root.getChildNodes();
			for(int i=0; i<nList.getLength(); i++){
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE){
					Element el = (Element) nNode;
					theString+=parseXMLChild(el, el.getNodeName())+"\n";
				}
			}
			return theString;

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return "";
		} catch (SAXException e) {
			e.printStackTrace();
			return "";
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}

	}

	private static String parseXMLChild(Element el, String parent){
		String theString = parent + " = ";
		NodeList nList = el.getChildNodes();
		
		Node first = nList.item(0);
		if(first.getNodeType() == Node.ATTRIBUTE_NODE || first.getNodeType() == Node.CDATA_SECTION_NODE){
			theString+=first.getNodeValue();
		}
		else{
			theString += "[ ";
			for(int i=0; i<nList.getLength()-1; i++){
				Element elChild = (Element) nList.item(i);
				theString+= parseXMLChild(elChild, elChild.getNodeName())+" , ";
			}
			//Last element
			Element elChild = (Element) nList.item(nList.getLength()-1);
			theString+= parseXMLChild(elChild, elChild.getNodeName());

			theString += " ]";
		}
		return theString;
	}
}
