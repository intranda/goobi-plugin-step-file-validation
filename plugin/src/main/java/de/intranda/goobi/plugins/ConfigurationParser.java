package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.goobi.beans.Step;
import org.jdom2.Namespace;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;
import lombok.Getter;

public class ConfigurationParser {
	private XMLConfiguration xmlConfig;
	private HierarchicalConfiguration parentConfig;
	@Getter
	private HashMap<String,ToolConfiguration> toolConfigurations;
	@Getter
	private List<List<Check>> ingestLevels;
	@Getter
	private HashMap<String, Namespace> namespaces;
	
	private String profile;
	@Getter
	private String fileFiler;
	@Getter
	private int targetLevel;

	public ConfigurationParser(String title, Step step) throws IllegalArgumentException {
		SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);
		this.parentConfig = myconfig.getParent();
		//read ProjectConfiguration
		this.profile = myconfig.getString("profileName", null);
		this.fileFiler = myconfig.getString("fileFilter",null);
		this.targetLevel = myconfig.getInteger("targetLevel",null);
		//read global Configuraton
		this.namespaces = readNamespaces(); 
		this.toolConfigurations = readToolConfigurations();
		this.ingestLevels = readProfile(profile);
		
	}
	

//	public ParseConfiguration (String filename) {
//		xmlConfig = new XMLConfiguration();
//        xmlConfig.setDelimiterParsingDisabled(true);
//        try {
//            xmlConfig.load(new Helper().getGoobiConfigDirectory() + filename);
//        } catch (ConfigurationException e) {
//          //  log.error("Error while reading the configuration file " + filename, e);
//        }
//        xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
//        xmlConfig.setExpressionEngine(new XPathExpressionEngine());
//        xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
//        
//    }
	
	public HashMap<String,Namespace> readNamespaces () {
		List<HierarchicalConfiguration> nodes = this.parentConfig.configurationsAt("global/namespaces/namespace");
		HashMap<String,Namespace> namespaces = new HashMap();
		for (HierarchicalConfiguration node : nodes) {
			String name = node.getString("@name", null);
			String uri = node.getString("@uri", null);

			Namespace ns = Namespace.getNamespace(name, uri);
			namespaces.put(name,ns);
		}
		return namespaces;
	}
	public List<List<Check>> readProfile(String profile) throws IllegalArgumentException {
		SubnodeConfiguration profileNode = this.parentConfig.configurationAt("//profile[@name='" + profile + "']" );
		List<HierarchicalConfiguration> level = profileNode.configurationsAt("level");
		List<List<Check>> ingestLevels = new ArrayList<List<Check>>();
		for (HierarchicalConfiguration node : level) {
			List<Check> checkList = new ArrayList<Check>();
			List<HierarchicalConfiguration> checkNodes = node.configurationsAt("check");
			for (HierarchicalConfiguration checkNode : checkNodes) {
				String tool = checkNode.getString("@tool", null);
				String name = checkNode.getString("@name", null);
				String dependsOn = checkNode.getString("@dependsOn", null);
				String code = checkNode.getString("@code", null);
				String xpathSelector = checkNode.getString("@xpathSelector", null);
				String regEx = checkNode.getString("@regEx", null);
				String xmlNamespace = node.getString("@xmlNamespace",null);
				//maybe it's handy to be able to redefine the namespace in the Check;
				//so we add this attribute
				if (xmlNamespace==null) {
					xmlNamespace =  this.toolConfigurations.get(tool).getXmlNamespace();
				} 
				Namespace namespace =null;
				if (xmlNamespace!=null) {
					namespace = this.namespaces.get(xmlNamespace);
				}
				
				Check check = new Check(name, dependsOn, tool, code, xpathSelector, regEx, namespace);
				checkList.add(check);
			}
			ingestLevels.add(checkList);
		}
		return ingestLevels;
	}

	public HashMap<String,ToolConfiguration> readToolConfigurations() {
		List<HierarchicalConfiguration> tools = this.parentConfig.configurationsAt("global/tools/tool");
		HashMap<String,ToolConfiguration> Configurations = new HashMap();
		for (HierarchicalConfiguration node : tools) {
			String name = node.getString("@name", null);
			String cmd = node.getString("@cmd", null);
			boolean stdout = node.getBoolean("@stdout", false);
			String xmlNamespace = node.getString("@xmlNamespace",null);
			ToolConfiguration tc = new ToolConfiguration(name, cmd, stdout, xmlNamespace);
			Configurations.put(name,tc);
		}
		return Configurations;
	}

//	public SubnodeConfiguration getProjectConfiguration(Step step) {
//		String projectName = step.getProzess().getProjekt().getTitel();
//		// find out the sub-configuration node for the right project and step
//		SubnodeConfiguration myconfig = null;
//		try {
//			myconfig = xmlConfig
//					.configurationAt("//config[./project = '" + projectName + "'][./step = '" + step.getTitel() + "']");
//		} catch (IllegalArgumentException e) {
//			try {
//				myconfig = xmlConfig.configurationAt("//config[./project = '*'][./step = '" + step.getTitel() + "']");
//			} catch (IllegalArgumentException e1) {
//				try {
//					myconfig = xmlConfig.configurationAt("//config[./project = '" + projectName + "'][./step = '*']");
//				} catch (IllegalArgumentException e2) {
//					myconfig = xmlConfig.configurationAt("//config[./project = '*'][./step = '*']");
//				}
//			}
//		}
//		return myconfig;
//	}

}
