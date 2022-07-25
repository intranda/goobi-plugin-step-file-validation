package de.intranda.goobi.plugins;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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

import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.compress.utils.FileNameUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.intranda.goobi.plugins.Logging.LoggerInterface;
import de.intranda.goobi.plugins.Logging.ProcessLogger;
import de.intranda.goobi.plugins.Reporting.Report;
import de.intranda.goobi.plugins.Reporting.ReportEntry;
import de.intranda.goobi.plugins.Validation.Check;
import de.intranda.goobi.plugins.Validation.CheckManager;
import de.intranda.goobi.plugins.Validation.ValueReader;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.StorageProviderInterface;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.ProcessManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.WriteException;

@PluginImplementation
@Log4j2
public class PdfValidationStepPlugin implements IStepPluginVersion2 {

	@Getter
	private String title = "intranda_step_pdf_validation";
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
			log.info("PdfValidation step plugin initialized");
		} else {
			log.info("Error initializing Plugin");
			this.logger.message("Error reading Configuration File", LogType.DEBUG);
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

	public static Report validateFile(Path path) {
		return validateFile(path, "*");
	}
	
	private static String removeFileExtension(String fileName) {
		if(fileName.lastIndexOf(".")>0) {
			 return fileName.substring(0, fileName.lastIndexOf('.'));
		}
		else return fileName;
				
	}

	public static Report validateFile(Path path, String institution) {
		ConfigurationParser confParser = null;
		Report report;
		try {
			confParser = new ConfigurationParser("intranda_step_pdf_validation", institution);
		} catch (IllegalArgumentException ex) {
			report = new Report(-1, "Error reading the Configuration File: " + ex.getMessage(),
					path.getFileName().toString(), new ArrayList<ReportEntry>());
		}
		String fileName= removeFileExtension(path.getFileName().toString());
		
		HashMap<String, ToolConfiguration> toolConfigurations = confParser.getToolConfigurations();
		List<List<Check>> levelWithChecks = confParser.getIngestLevelChecks();
		List<List<ValueReader>> levelWithReaders = confParser.getIngestLevelReader();
		Path outputPath = Paths.get(path.getParent().toString(),fileName);

		// TODO more Checks for the Path maybe with Filter...
		if (StorageProvider.getInstance().isFileExists(path)) {
			CheckManager CManager = new CheckManager(toolConfigurations, levelWithChecks, levelWithReaders, outputPath);
			report = CManager.runChecks(confParser.getTargetLevel(), path);
		} else {
			report = new Report(-1, "The file could not be found!", path.getFileName().toString(),
					new ArrayList<ReportEntry>());
		}
		return report;
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
			
			CheckManager CManager = new CheckManager(tools, levelChecks, levelValueReaders, outputRootPath, inputFolder,
					cParser.getFileFiler());
			CManager.addLogger(this.logger);
			List<Report> reports = CManager.runChecks(cParser.getTargetLevel());
			if (reports.isEmpty()) {
				this.logger.message("ERROR: No Report was created!", LogType.INFO);
			}
			for (Report report : reports) {
				if (report.getLevel() < cParser.getTargetLevel()) {
					this.logger.message(
							"ERROR: The File " + report.getFileName() + " did not reach the required target level!",
							LogType.ERROR);
					successful = false;
				} else {
					this.logger.message("The File " + report.getFileName() + " reached the required target level!",
							LogType.INFO);
				}
			}
			
			if (cParser.isWriteResult()) {
				for(Report report : reports) {
					JAXBContext jaxbContext = JAXBContext.newInstance(Report.class);
					Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
					jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
					StringWriter sw = new StringWriter();
					jaxbMarshaller.marshal(report, sw);
					
					//Check if Folder exists and if not try to create it
					Path outputPath = CManager.getOutputPath();
					StorageProviderInterface SPI = StorageProvider.getInstance();
					if (!SPI.isFileExists(outputPath))
						SPI.createDirectories(outputPath);
					String fileName = removeFileExtension(report.getFileName());
					fileName = fileName+"-result.xml";
					
					Path FileOutputPath = outputPath.resolve(fileName);
					jaxbMarshaller.marshal(report, new File(FileOutputPath.toString()));
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
		} catch (IOException | InterruptedException | SwapException | DAOException | PreferencesException e) {
			successful = false;
		} catch (JAXBException e) {
			logger.message(e.getMessage(), LogType.DEBUG);
			successful = false;
			logger.message("Error writing report to filesystem", LogType.DEBUG);
		} catch (ReadException | WriteException e) {
			successful = false;
			logger.message("Error opening Preferences" + e.getMessage(), LogType.DEBUG);
		}
		log.info("PdfValidation step plugin executed");
		if (!successful) {
			return PluginReturnValue.ERROR;
		}
		return PluginReturnValue.FINISH;
	}
}
