package org.goobi.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.goobi.beans.Step;
import org.goobi.validation.Check;
import org.goobi.validation.ValueReader;
import org.jdom2.Namespace;

import de.sub.goobi.config.ConfigPlugins;
import lombok.Getter;

public class ConfigurationParser {
    private XMLConfiguration xmlConfig;
    private HierarchicalConfiguration parentConfig;
    @Getter
    private HashMap<String, ToolConfiguration> toolConfigurations;
    @Getter
    private List<List<Check>> ingestLevelChecks;
    @Getter
    private List<List<ValueReader>> ingestLevelReader;
    @Getter
    private HashMap<String, Namespace> namespaces;
    @Getter
    private String fileFiler;
    @Getter
    private int targetLevel;
    @Getter
    private String outputFolder;
    @Getter
    private String inputFolder;
    @Getter
    private boolean writeResult;

    private List<String> parsedChecks = new ArrayList<>();

    public ConfigurationParser(String title, Step step) throws IllegalArgumentException {
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);
        this.writeResult = myconfig.getBoolean("writeReport", true);
        initialize(myconfig);
    }

    public ConfigurationParser(String pluginName, String institution) {
        this.xmlConfig = ConfigPlugins.getPluginConfig(pluginName);
        this.xmlConfig.setExpressionEngine(new XPathExpressionEngine());
        SubnodeConfiguration myconfig = getConfiguration(institution);
        initialize(myconfig);

    }

    private void initialize(SubnodeConfiguration myconfig) {
        // get parentNode
        this.parentConfig = myconfig.getParent();
        // read ProjectConfiguration
        String profile = myconfig.getString("profileName", null);
        this.fileFiler = myconfig.getString("fileFilter", null);
        this.targetLevel = myconfig.getInteger("targetLevel", null);
        // read global Configuraton
        this.namespaces = readNamespaces();
        this.toolConfigurations = readToolConfigurations();
        this.ingestLevelChecks = readChecks(profile);
        this.ingestLevelReader = readValueReader(profile);
        this.inputFolder = myconfig.getString("inputFolder", "");
        this.outputFolder = myconfig.getString("outputFolder", "");
    }

    private HashMap<String, Namespace> readNamespaces() {
        List<HierarchicalConfiguration> nodes = this.parentConfig.configurationsAt("global/namespaces/namespace");
        HashMap<String, Namespace> nsMap = new HashMap<>();
        for (HierarchicalConfiguration node : nodes) {
            String name = node.getString("@name", null);
            String uri = node.getString("@uri", null);

            Namespace ns = Namespace.getNamespace(name, uri);
            nsMap.put(name, ns);
        }
        return nsMap;
    }

    private List<HierarchicalConfiguration> readProfile(String profile) {
        SubnodeConfiguration profileNode = this.parentConfig.configurationAt("//profile[@name='" + profile + "']");
        return profileNode.configurationsAt("level");
    }

    private List<List<Check>> readChecks(String profile) throws IllegalArgumentException {
        List<List<Check>> ingestLevels = new ArrayList<>();
        List<HierarchicalConfiguration> level = readProfile(profile);
        for (HierarchicalConfiguration node : level) {

            List<Check> checkList = new ArrayList<>();
            List<HierarchicalConfiguration> checkNodes = node.configurationsAt("check");
            for (HierarchicalConfiguration checkNode : checkNodes) {
                String name = checkNode.getString("@name", null);
                String tool = checkNode.getString("@tool", null);
                if (this.toolConfigurations.get(tool)== null) {
                    throw new IllegalArgumentException(
                            "You have to define a tool with the name:" + tool + " checkName: " + name + " Please update the configuration file.");
                }
                String dependsOn = checkNode.getString("@dependsOn", null);
                String group = checkNode.getString("@group", null);
                if (dependsOn != null && parsedChecks.stream().noneMatch(checkName -> checkName.equals(dependsOn))) {
                    throw new IllegalArgumentException(
                            "You can't depend on a Check you have not defined yet-> dependsOn:" + dependsOn + " checkName: " + name);
                }

                String code = checkNode.getString("@code", null);
                String xpathSelector = checkNode.getString("@xpathSelector", null);
                String regEx = checkNode.getString("@regEx", null);
                String xmlNamespace = node.getString("@xmlNamespace", null);
                // maybe it's handy to be able to redefine the namespace in the Check;
                // so we add this attribute
                if (xmlNamespace == null) {
                    xmlNamespace = this.toolConfigurations.get(tool).getXmlNamespace();
                }
                Namespace namespace = null;
                if (xmlNamespace != null) {
                    namespace = this.namespaces.get(xmlNamespace);
                }

                Check check = new Check(name, dependsOn, group, tool, code, xpathSelector, regEx, namespace);
                checkList.add(check);
                parsedChecks.add(name);
            }
            ingestLevels.add(checkList);
        }
        return ingestLevels;
    }

    private List<List<ValueReader>> readValueReader(String profile) throws IllegalArgumentException {
        List<List<ValueReader>> ingestLevelReaders = new ArrayList<>();
        List<HierarchicalConfiguration> level = readProfile(profile);
        for (HierarchicalConfiguration node : level) {

            List<ValueReader> readerList = new ArrayList<>();
            List<HierarchicalConfiguration> valueReaderNodes = node.configurationsAt("setValue");
            for (HierarchicalConfiguration checkNode : valueReaderNodes) {
                String tool = checkNode.getString("@tool", null);
                String name = checkNode.getString("@name", null);
                if (this.toolConfigurations.get(tool)== null) {
                    throw new IllegalArgumentException(
                            "You have to define a tool with the name:" + tool + " checkName: " + name + " Please update the configuration file.");
                }
                String dependsOn = checkNode.getString("@dependsOn", null);
                if (dependsOn == null) {
                    throw new IllegalArgumentException("setValue Elements have to depend on a Check. Please correct setValue-Element: " + name);
                }
                String group = null;
                if (parsedChecks.stream().noneMatch(checkName -> checkName.equals(dependsOn))) {
                    throw new IllegalArgumentException(
                            "You can't depend on a Check you have not defined -> dependsOn:" + dependsOn + " checkName: " + name);
                }
                String code = checkNode.getString("@code", null);
                String xpathSelector = checkNode.getString("@xpathSelector", null);
                String regEx = null;
                // maybe it's handy to be able to redefine the namespace in the Check
                // so we add this attribute
                String xmlNamespace = node.getString("@xmlNamespace", null);

                String mets = checkNode.getString("@mets", null);
                String processProperty = checkNode.getString("@processProperty", null);
                if (xmlNamespace == null) {
                    xmlNamespace = this.toolConfigurations.get(tool).getXmlNamespace();
                }
                Namespace namespace = null;
                if (xmlNamespace != null) {
                    namespace = this.namespaces.get(xmlNamespace);
                }

                ValueReader reader = new ValueReader(name, dependsOn, group, tool, code, xpathSelector, regEx, namespace, processProperty, mets);
                readerList.add(reader);
            }
            ingestLevelReaders.add(readerList);
        }
        return ingestLevelReaders;
    }

    private HashMap<String, ToolConfiguration> readToolConfigurations() {
        List<HierarchicalConfiguration> tools = this.parentConfig.configurationsAt("global/tools/tool");
        HashMap<String, ToolConfiguration> configurations = new HashMap<>();
        for (HierarchicalConfiguration node : tools) {
            String name = node.getString("@name", null);
            String cmd = node.getString("@cmd", null);
            boolean stdout = node.getBoolean("@stdout", false);
            String xmlNamespace = node.getString("@xmlNamespace", null);
            ToolConfiguration tc = new ToolConfiguration(name, cmd, stdout, xmlNamespace);
            configurations.put(name, tc);
        }
        return configurations;
    }

    private SubnodeConfiguration getConfiguration(String institution) {
        // find out the sub-configuration node for the right institution
        SubnodeConfiguration myconfig = null;
        try {
            myconfig = xmlConfig.configurationAt("//config[./institution = '" + institution + "']");
        } catch (IllegalArgumentException e) {
            myconfig = xmlConfig.configurationAt("//config[./institution = '*']");
        }
        return myconfig;
    }
}
