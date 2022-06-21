package de.intranda.goobi.plugins;

import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.goobi.beans.Step;

import de.intranda.goobi.plugins.PdfValidationStepPlugin.Check;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;

public class ParseConfiguration {
	private XMLConfiguration xmlConfig;
	
	public ParseConfiguration (String title, Step step) {
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);
        String profile = myconfig.getString("profile", null);
        HierarchicalConfiguration parentConfig = myconfig.getParent();
        getProfile(profile, parentConfig);
	}
	
	public ParseConfiguration (String filename) {
		xmlConfig = new XMLConfiguration();
        xmlConfig.setDelimiterParsingDisabled(true);
        try {
            xmlConfig.load(new Helper().getGoobiConfigDirectory() + filename);
        } catch (ConfigurationException e) {
          //  log.error("Error while reading the configuration file " + filename, e);
        }
        xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
        xmlConfig.setExpressionEngine(new XPathExpressionEngine());
        xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
        
    }
	
	private void getProfile(String profile, HierarchicalConfiguration parentConfig) {
		SubnodeConfiguration profileNode = parentConfig.configurationAt("global/profile@name=\""+ profile +"\"");
	}
	
	
	public SubnodeConfiguration getProjectConfiguration(Step step) {
		String projectName = step.getProzess().getProjekt().getTitel();
		 // find out the sub-configuration node for the right project and step 
        SubnodeConfiguration myconfig = null;
        try {
            myconfig = xmlConfig
                    .configurationAt("//config[./project = '" + projectName + "'][./step = '" + step.getTitel() + "']");
        } catch (IllegalArgumentException e) {
            try {
                myconfig = xmlConfig.configurationAt("//config[./project = '*'][./step = '" + step.getTitel() + "']");
            } catch (IllegalArgumentException e1) {
                try {
                    myconfig = xmlConfig.configurationAt("//config[./project = '" + projectName + "'][./step = '*']");
                } catch (IllegalArgumentException e2) {
                    myconfig = xmlConfig.configurationAt("//config[./project = '*'][./step = '*']");
                }
            }
        }
        return myconfig;
	}
	
}
