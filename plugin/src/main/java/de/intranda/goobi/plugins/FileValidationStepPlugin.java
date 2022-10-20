package de.intranda.goobi.plugins;

/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.configuration.ConfigurationParser;
import org.goobi.configuration.ToolConfiguration;
import org.goobi.files.FileUtils;
import org.goobi.logging.LoggerInterface;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;
import org.goobi.reporting.Report;
import org.goobi.validation.Check;
import org.goobi.validation.CheckManager;
import org.goobi.validation.ValueReader;

import de.intranda.goobi.plugins.logging.ProcessLogger;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.StorageProviderInterface;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.ProcessManager;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;

@PluginImplementation
@Log4j2
public class FileValidationStepPlugin implements IStepPluginVersion2 {

    @Getter
    private String title = "intranda_step_file_validation";
    @Getter
    private Step step;
    @Getter
    private String value;
    private LoggerInterface logger;
    private String returnPath;
    private List<List<Check>> levelChecks;
    private List<List<ValueReader>> levelValueReaders;
    private HashMap<String, ToolConfiguration> tools;
    private Process process;
    private ConfigurationParser cParser;

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;
        this.process = ProcessManager.getProcessById(step.getProcessId());
        this.logger = new ProcessLogger(process);

        cParser = new ConfigurationParser(title, step);

        this.tools = cParser.getToolConfigurations();
        this.levelChecks = cParser.getIngestLevelChecks();
        this.levelValueReaders = cParser.getIngestLevelReader();

        if (tools.size() > 0 && levelChecks.size() > 0) {
            log.info("fileValidation step plugin initialized");
        } else {
            log.info("Error initializing Plugin");
            this.logger.message("Error reading configuration file", LogType.DEBUG);
        }
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return "/uii/plugin_step_pdf_validation.xhtml";
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String cancel() {
        return "/uii" + returnPath;
    }

    @Override
    public String finish() {
        return "/uii" + returnPath;
    }

    @Override
    public int getInterfaceVersion() {
        return 0;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null;
    }

    @Override
    public boolean execute() {
        PluginReturnValue ret = run();
        return ret != PluginReturnValue.ERROR;
    }

    @Override
    public PluginReturnValue run() {
        boolean successful = true;
        this.process = ProcessManager.getProcessById(step.getProcessId());

        try {
            Fileformat ff = step.getProzess().readMetadataFile();

            Prefs prefs = step.getProzess().getRegelsatz().getPreferences();
            VariableReplacer replacer = new VariableReplacer(ff.getDigitalDocument(), prefs, step.getProzess(), step);
            String outputRootPath = replacer.replace(cParser.getOutputFolder());
            String inputFolder = replacer.replace(cParser.getInputFolder());

            CheckManager cManager = new CheckManager(tools, levelChecks, levelValueReaders, outputRootPath, inputFolder, cParser.getFileFiler());
            cManager.addLogger(this.logger);
            List<Report> reports = cManager.runChecks(cParser.getTargetLevel());
            if (reports.isEmpty()) {
                this.logger.message("ERROR: No report was created!", LogType.INFO);
                successful = false;
            }
            for (Report report : reports) {
                if (report.getLevel() < cParser.getTargetLevel()) {
                    this.logger.message("ERROR: The file " + report.getFileName() + " only reached level " + report.getLevel()
                            + " the required target level of " + cParser.getTargetLevel() + "!", LogType.ERROR);
                    successful = false;
                } else {
                    this.logger.message("SUCCESS: The file " + report.getFileName() + " reached target level " + report.getLevel() + "! Level "
                            + cParser.getTargetLevel() + " was required!", LogType.INFO);
                }
            }

            if (cParser.isWriteResult()) {
                for (Report report : reports) {
                    JAXBContext jaxbContext = JAXBContext.newInstance(Report.class);
                    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                    StringWriter sw = new StringWriter();
                    jaxbMarshaller.marshal(report, sw);

                    //Check if Folder exists and if not try to create it
                    Path outputPath = cManager.getOutputPath();
                    StorageProviderInterface spi = StorageProvider.getInstance();
                    if (!spi.isFileExists(outputPath)) {
                        spi.createDirectories(outputPath);
                    }
                    String fileName = FileUtils.removeFileExtension(report.getFileName());
                    fileName = fileName + "-result.xml";

                    Path fileOutputPath = outputPath.resolve(fileName);
                    jaxbMarshaller.marshal(report, new File(fileOutputPath.toString()));
                }
            }

            if (successful) {
                MetadataWriter metadataWriter = new MetadataWriter(process);
                metadataWriter.addLogger(this.logger);
                metadataWriter.writeReportResults(reports);
            }

        } catch (MetadataWriterException ex) {
            logger.message(ex.getMessage(), LogType.ERROR);
            successful = false;
        } catch (IOException | InterruptedException | SwapException | PreferencesException e) {
            successful = false;
        } catch (JAXBException e) {
            logger.message(e.getMessage(), LogType.DEBUG);
            successful = false;
            logger.message("Error writing report to filesystem", LogType.DEBUG);
        } catch (ReadException e) {
            successful = false;
            logger.message("Error opening preferences" + e.getMessage(), LogType.DEBUG);
        }
        log.info("PdfValidation step plugin executed");
        if (!successful) {
            return PluginReturnValue.ERROR;
        }
        return PluginReturnValue.FINISH;
    }
}
