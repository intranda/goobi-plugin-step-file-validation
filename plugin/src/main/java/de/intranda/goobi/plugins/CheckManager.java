package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.goobi.beans.Process;
import org.goobi.production.enums.LogType;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import de.intranda.goobi.plugins.Logging.LoggerInterface;
import de.intranda.goobi.plugins.Reporting.Report;
import de.intranda.goobi.plugins.Reporting.ReportEntry;
import de.sub.goobi.helper.StorageProvider;

import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;

public class CheckManager {

	private HashMap<String, ToolConfiguration> toolConfigurations;
	private List<List<Check>> ingestLevels;

	private Path outputPath;
	private List<Path> pdfsInFolder = new ArrayList<>();
	private List<LoggerInterface> loggers = new ArrayList<>();
	private HashMap<String, List<Check>> checksGroupedByDependsOn = new LinkedHashMap<>();

	// mayber refactor later

	public CheckManager(HashMap<String, ToolConfiguration> toolsConfigurations, List<List<Check>> ingestLevels,
			Process process, String fileFilter) throws IOException, InterruptedException, SwapException, DAOException {

		this.toolConfigurations = toolsConfigurations;
		this.ingestLevels = ingestLevels;

		// readConfiguration

		// TODO implement FilenameFilter here!
		// this.pdfsInFolder.addAll(StorageProvider.getInstance().listFiles(process.getSourceDirectory()));
		this.pdfsInFolder.addAll(StorageProvider.getInstance().listFiles("/opt/digiverso/pdf", (path) -> {
			try {
				if (path.getFileName().toString().matches(fileFilter)) {

					return !Files.isHidden(path) && Files.isRegularFile(path) && Files.isReadable(path);

				} else
					return false;
			} catch (IOException e) {
				return false;
			}
		}));
		// TODO fix this
		String test = process.getProcessDataDirectory();
		this.outputPath = Paths.get(test, "validation", System.currentTimeMillis() + "_xml");
	}

	public CheckManager(HashMap<String, ToolConfiguration> toolsConfigurations, List<List<Check>> ingestLevels) {
		this.toolConfigurations = toolsConfigurations;
		this.ingestLevels = ingestLevels;
		// pdfsfolder
	}

	public void addLogger(LoggerInterface logger) {
		loggers.add(logger);
	}

	public void log(String message, LogType type) {
		for (LoggerInterface logger : loggers) {
			logger.message(message, type);
		}
	}

	private SimpleEntry<String, String> runTool(String toolName, Path pdfFile)
			throws IOException, InterruptedException {
		ToolConfiguration tc = this.toolConfigurations.get(toolName);
		ToolRunner tr = new ToolRunner(tc, outputPath);
		return tr.runTool(pdfFile);
	}

	private void groupChecksByDependsOn() {
		for (int level = 0; level < ingestLevels.size(); level++) {
			List<Check> checks = ingestLevels.get(level);
			for (Check check : checks) {
				String dependsOn = check.getDependsOn();
				if (dependsOn == null)
					continue;
				List<Check> groupedChecks = checksGroupedByDependsOn.get(dependsOn);
				if (groupedChecks == null) {
					groupedChecks = new ArrayList();
					checksGroupedByDependsOn.put(dependsOn, groupedChecks);
				}
				groupedChecks.add(check);
			}
		}
	}

	private HashMap<String, List<Check>> groupChecksByGroup(List<Check> checks) {
		HashMap<String, List<Check>> checksGroupedByGroup = new HashMap<String, List<Check>>();
		for (Check check : checks) {
			String group = check.getGroup();
			if (group == null)
				continue;
			List<Check> groupedChecks = checksGroupedByGroup.get(group);
			if (groupedChecks == null) {
				groupedChecks = new ArrayList();
				checksGroupedByGroup.put(group, groupedChecks);
			}
			groupedChecks.add(check);
		}
		return checksGroupedByGroup;
	}

	private void updateDependencies(String checkName) {
		List<Check> checks = checksGroupedByDependsOn.get(checkName);
		if (checks == null)
			return;
		for (Check check : checks) {
			check.setStatus(CheckStatus.PREQUISITEFAILED);
			updateDependencies(check.getName());
		}
	}

	public Report runChecks(int targetLevel, Path pathToFile) {
		int reachedLevel = -1;
		String fileName = pathToFile.getFileName().toString();
		SAXBuilder jdomBuilder = new SAXBuilder();
		HashMap<String, SimpleEntry<String, String>> xmlReportsByTool = new HashMap<>();
		HashMap<String, Document> jdomDocumentsByTool = new HashMap<>();
		List<ReportEntry> reportEntries = new ArrayList<>();
		groupChecksByDependsOn();
		for (int level = 0; level < ingestLevels.size() && level <= targetLevel; level++) {
			List<Check> checks = ingestLevels.get(level);
			HashMap<String, List<Check>> checksGroupedByGroup = groupChecksByGroup(checks);
			try {
				for (Check check : checks) {
					if (check.getStatus() != CheckStatus.NEW) {
						continue;
					}
					String toolName = check.getTool();
					Document jdomDocument = jdomDocumentsByTool.get(toolName);

					if (jdomDocument == null) {
						SimpleEntry<String, String> reportFile = runTool(toolName, pathToFile);
						jdomDocument = jdomBuilder.build(reportFile.getValue());
						jdomDocumentsByTool.put(toolName, jdomDocument);
					}

					ReportEntry re = check.run(jdomDocument);
					reportEntries.add(re);
					if (re.getStatus() != CheckStatus.SUCCESS) {
						log("Check '" + check.getName() + "' failed! The Errormessage is " + check.getCode(),
								LogType.ERROR);
						updateDependencies(check.getName());
						if (groupFailed(check.getGroup(), checksGroupedByGroup))
							return new Report(reachedLevel, check.getCode(), fileName, reportEntries);
					}
				}
				reachedLevel = level;
			} catch (IOException | JDOMException | InterruptedException e) {
				log("A Check failed because of an Exception. ErrorMessage: " + e.getMessage(), LogType.ERROR);
				return new Report(reachedLevel, "Error running tool or reading report file", fileName, reportEntries);
			}
		}
		return new Report(reachedLevel, null, fileName, reportEntries);
	}

	/**
	 * @param group                group which shall be tested
	 * @param checksGroupedByGroup HashMap with groups of the current level
	 * @return true if ALL checks have one of the following statis ERROR,
	 *         PREQUISITEFAILED, FAILED function also returns true if the given
	 *         group was null or if no group with the given name could be found in
	 *         the provided HashMap.
	 */
	private boolean groupFailed(String group, HashMap<String, List<Check>> checksGroupedByGroup) {
		if (group == null) {
			return true;
		}
		List<Check> checks = checksGroupedByGroup.get(group);
		if (checks == null || checks.size() == 1) {
			return true;
		}
		return checks.stream().allMatch(check -> check.getStatus() == CheckStatus.ERROR
				|| check.getStatus() == CheckStatus.PREQUISITEFAILED || check.getStatus() == CheckStatus.FAILED);
	}

	public List<Report> runChecks(int targetLevel) throws IOException, InterruptedException {
		List<Report> reports = new ArrayList<>();
		for (Path pdfFile : this.pdfsInFolder) {
			reports.add(runChecks(targetLevel, pdfFile));
		}
		return reports;
	}
}
