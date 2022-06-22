package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.goobi.beans.Step;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;

public class ParseConfiguration {
	private XMLConfiguration xmlConfig;
	private HierarchicalConfiguration parentConfig;
	private List<ToolConfiguration> toolConfigurations;
	private List<List<Check>> ingestLevels;

	public ParseConfiguration(String title, Step step) {
		SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);
		String profile = myconfig.getString("profileName", null);
		this.parentConfig = myconfig.getParent();
		this.ingestLevels = readProfile(profile);
		this.toolConfigurations = readToolConfigurations();
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

	private List<List<Check>> readProfile(String profile) throws IllegalArgumentException {
		SubnodeConfiguration profileNode = this.parentConfig.configurationAt("//profile[@name='" + profile + "']" );
		List<HierarchicalConfiguration> level = profileNode.configurationsAt("level");
		List<List<Check>> ingestLevels = new ArrayList<List<Check>>();
		for (HierarchicalConfiguration node : level) {
			List<Check> checkList = new ArrayList<Check>();
			List<HierarchicalConfiguration> checkNodes = node.configurationsAt("check");
			for (HierarchicalConfiguration checkNode : checkNodes) {
				String tool = checkNode.getString("@tool", null);
				String name = checkNode.getString("@name", null);
				String code = checkNode.getString("@code", null);
				String xpathSelector = checkNode.getString("@xpathSelector", null);
				if (validateXPathSelector(xpathSelector))
					throw new IllegalArgumentException("The xpath-Expression " + xpathSelector + " is invalid!");
				String regEx = checkNode.getString("@regEx", null);
				Check check = new Check(name, tool, code, xpathSelector, regEx);
				checkList.add(check);
			}
			ingestLevels.add(checkList);
		}
		return ingestLevels;
	}
	
	private boolean validateXPathSelector(String expression) {
		XPathFactory factory =  XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		try {
			XPathExpression expr = xpath.compile(expression);
			return true;
		} catch (XPathExpressionException e) {
			return false;
		}
	}

	private List<ToolConfiguration> readToolConfigurations() {
		List<HierarchicalConfiguration> tools = this.parentConfig.configurationsAt("global/tools/tool");
		List<ToolConfiguration> toolConfigurations = new ArrayList<ToolConfiguration>();
		for (HierarchicalConfiguration node : tools) {
			String name = node.getString("@name", null);
			String configFolder = node.getString("@configFolder");
			ToolConfiguration tc = new ToolConfiguration(name, configFolder);
			toolConfigurations.add(tc);
		}
		return toolConfigurations;
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
