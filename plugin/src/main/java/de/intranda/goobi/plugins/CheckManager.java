package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.goobi.beans.Process;
import org.goobi.production.enums.LogType;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import de.intranda.goobi.plugins.Logging.LoggerInterface;
import de.intranda.goobi.plugins.Reporting.Report;
import de.intranda.goobi.plugins.Reporting.ReportEntry;
import de.intranda.goobi.plugins.Reporting.ReportEntryStatus;
import de.sub.goobi.helper.StorageProvider;

import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;

public class CheckManager {

	private HashMap<String, ToolConfiguration> toolConfigurations;
	private List<List<Check>> ingestLevels;

	private Path outputPath;
	private List<Path> pdfsInFolder = new ArrayList<>();
	private List<LoggerInterface> loggers = new ArrayList<>();
	// mayber refactor later
	
	public void addLogger (LoggerInterface logger) {
		loggers.add(logger);
	}
	
	public void log(String message, LogType type) {
		for (LoggerInterface logger: loggers) {
			logger.message(message, type);
		}
	}
	
	public CheckManager(HashMap<String, ToolConfiguration> toolsConfigurations, List<List<Check>> ingestLevels,
			Process process, String fileFilter) throws IOException, InterruptedException, SwapException, DAOException {

		this.toolConfigurations = toolsConfigurations;
		this.ingestLevels = ingestLevels;

		// readConfiguration
	
		// TODO implement FilenameFilter here!
		//this.pdfsInFolder.addAll(StorageProvider.getInstance().listFiles(process.getSourceDirectory()));
		this.pdfsInFolder.addAll(StorageProvider.getInstance().listFiles("/opt/digiverso/pdf", (path)-> {
			try {
			if (path.getFileName().toString().matches(fileFilter)) {
			
				return !Files.isHidden(path)&&Files.isRegularFile(path)&&Files.isReadable(path);

			}else return false;
			} catch (IOException e) {
				return false;
			}}));
		// TODO fix this
		String test = process.getProcessDataDirectory();
		this.outputPath = Paths.get(test, "validation", System.currentTimeMillis() + "_xml");
	}

	public CheckManager(HashMap<String, ToolConfiguration> toolsConfigurations, List<List<Check>> ingestLevels) {
		this.toolConfigurations = toolsConfigurations;
		this.ingestLevels = ingestLevels;
		// pdfsfolder
	}

	private SimpleEntry<String, String> runTool(String toolName, Path pdfFile)
			throws IOException, InterruptedException {
		ToolConfiguration tc = this.toolConfigurations.get(toolName);
		ToolRunner tr = new ToolRunner(tc, outputPath);
		return tr.runTool(pdfFile);
	}

	public Report runChecks(int targetLevel, Path pathToFile) {
		int reachedLevel = -1;
		String fileName= pathToFile.getFileName().toString();
		SAXBuilder jdomBuilder = new SAXBuilder();
		HashMap<String, SimpleEntry<String,String>> xmlReports = new HashMap();
		List<ReportEntry> reportEntries = new ArrayList<>();
		for (int level = 0; level < ingestLevels.size()&&level<=targetLevel; level++) {
			List<Check> checks = ingestLevels.get(level);
			HashMap<String, List<Check>> ChecksGroupedByTool = new HashMap();
			for (Check check : checks) {
				String tool = check.getTool();
				List<Check> groupedChecks = ChecksGroupedByTool.get(tool);
				if (groupedChecks == null) {
					groupedChecks = new ArrayList();
					ChecksGroupedByTool.put(tool, groupedChecks);
				}
				groupedChecks.add(check);
			}
			try {
				//run grouped Checks
				for (String toolName : ChecksGroupedByTool.keySet()) {
					SimpleEntry<String,String> reportFile = xmlReports.get(toolName);
					if (reportFile == null) {
						reportFile = runTool(toolName, pathToFile);
						xmlReports.put(toolName, reportFile);
					}
					Document jdomDocument;
					jdomDocument = jdomBuilder.build(reportFile.getValue());

					for (Check check : ChecksGroupedByTool.get(toolName)) {
						ReportEntry re = check.check(jdomDocument);
						reportEntries.add(re);
						if (re.getStatus() != ReportEntryStatus.SUCCESS) {
							log("Check '"+ check.getName() +"' failed! The Errormessage is "+check.getCode(),LogType.ERROR);
							return new Report(reachedLevel, check.getCode(),fileName, reportEntries);
						}
					}
				}
				reachedLevel = level;
			} catch (IOException | JDOMException | InterruptedException e) {
				log("A Check failed because of an Exception. ErrorMessage: "+e.getMessage() ,LogType.ERROR);
				return new Report(reachedLevel, "Error running tool or reading report file",fileName, reportEntries);
			}
		}
		return new Report(reachedLevel, null ,fileName,reportEntries);
	}

	public List<Report> runChecks(int targetLevel) throws IOException, InterruptedException {
		List<Report> reports = new ArrayList<>();
		for (Path pdfFile : this.pdfsInFolder) {
			reports.add(runChecks(targetLevel, pdfFile));
		}
		return reports;
	}
}
