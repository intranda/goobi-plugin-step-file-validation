package org.goobi.validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.goobi.configuration.ToolConfiguration;
import org.goobi.files.ToolRunner;
import org.goobi.logging.LoggerInterface;
import org.goobi.production.enums.LogType;
import org.goobi.reporting.MetadataEntry;
import org.goobi.reporting.Report;
import org.goobi.reporting.ReportEntry;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import de.sub.goobi.helper.StorageProvider;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CheckManager {

    private Map<String, ToolConfiguration> toolConfigurations;
    private List<List<Check>> ingestLevelChecks;
    private List<List<ValueReader>> ingestLevelReaders;
    @Getter
    private Path outputPath;
    private List<Path> pdfsInFolder = new ArrayList<>();
    private List<LoggerInterface> loggers = new ArrayList<>();
    private HashMap<String, List<Check>> checksGroupedByDependsOn = new LinkedHashMap<>();
    private boolean runAllChecks = false;
    private String inputFolder = "";

    private CheckManager(Map<String, ToolConfiguration> toolsConfigurations, List<List<Check>> ingestLevelChecks,
            List<List<ValueReader>> ingestLevelReaders, String outputPath) {
        this.toolConfigurations = toolsConfigurations;
        this.ingestLevelChecks = ingestLevelChecks;
        this.ingestLevelReaders = ingestLevelReaders;
        this.outputPath = Paths.get(outputPath, System.currentTimeMillis() + "_xml");
    }

    public CheckManager(Map<String, ToolConfiguration> toolsConfigurations, List<List<Check>> ingestLevels,
            List<List<ValueReader>> ingestLevelReaders, Path outputPath) {
        this(toolsConfigurations, ingestLevels, ingestLevelReaders, outputPath.toString());

    }

    public CheckManager(Map<String, ToolConfiguration> toolsConfigurations, List<List<Check>> ingestLevels,
            List<List<ValueReader>> ingestLevelReaders, String outputPath, String inputFolder, String fileFilter) {
        this(toolsConfigurations, ingestLevels, ingestLevelReaders, outputPath);
        this.pdfsInFolder.addAll(StorageProvider.getInstance().listFiles(inputFolder, (path) -> {
            try {
                if (path.getFileName().toString().matches(fileFilter)) {

                    return !Files.isHidden(path) && Files.isRegularFile(path) && Files.isReadable(path);

                } else {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
        }));
        this.inputFolder = inputFolder;
    }

    /**
     * allows to add a logger of Type LoggerInterface to the Class
     * 
     * @param logger
     */
    public void addLogger(LoggerInterface logger) {
        loggers.add(logger);
    }

    private void log(String message, LogType type) {
        for (LoggerInterface logger : loggers) {
            logger.message(message, type);
        }
    }

    private SimpleEntry<String, String> runTool(String toolName, Path pdfFile) throws IOException, InterruptedException {
        ToolConfiguration tc = this.toolConfigurations.get(toolName);
        ToolRunner tr = new ToolRunner(tc, outputPath);
        return tr.runTool(pdfFile);
    }

    private void groupChecksByDependsOn() {
        for (int level = 0; level < ingestLevelChecks.size(); level++) {
            List<Check> checks = new ArrayList<>();
            checks.addAll(ingestLevelChecks.get(level));
            checks.addAll(ingestLevelReaders.get(level));
            for (Check check : checks) {
                String dependsOn = check.getDependsOn();
                if (dependsOn == null) {
                    continue;
                }
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
        HashMap<String, List<Check>> checksGroupedByGroup = new HashMap<>();
        for (Check check : checks) {
            String group = check.getGroup();
            if (group == null) {
                continue;
            }
            List<Check> groupedChecks = checksGroupedByGroup.get(group);
            if (groupedChecks == null) {
                groupedChecks = new ArrayList<>();
                checksGroupedByGroup.put(group, groupedChecks);
            }
            groupedChecks.add(check);
        }
        return checksGroupedByGroup;
    }

    private void updateDependencies(String checkName) {
        List<Check> checks = checksGroupedByDependsOn.get(checkName);
        if (checks == null) {
            return;
        }
        for (Check check : checks) {
            check.setStatus(CheckStatus.PREQUISITEFAILED);
            updateDependencies(check.getName());
        }
    }

    private Report addReaderReport(int endCheckOnLevel, Path pathToFile, Report report, HashMap<String, Document> jdomDocumentsByTool) {
        SAXBuilder jdomBuilder = new SAXBuilder();
        jdomBuilder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        jdomBuilder.setFeature("http://xml.org/sax/features/external-general-entities", false);
        jdomBuilder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        List<MetadataEntry> metadataEntries = new ArrayList<>();
        for (int level = 0; level <= endCheckOnLevel; level++) {
            List<ValueReader> valueReaders = ingestLevelReaders.get(level);
            try {
                for (ValueReader valueReader : valueReaders) {
                    if (valueReader.getStatus() != CheckStatus.NEW) {
                        continue;
                    }
                    String toolName = valueReader.getTool();
                    Document jdomDocument = jdomDocumentsByTool.get(toolName);

                    if (jdomDocument == null) {
                        SimpleEntry<String, String> reportFile = runTool(toolName, pathToFile);
                        jdomDocument = jdomBuilder.build(reportFile.getValue());
                        jdomDocumentsByTool.put(toolName, jdomDocument);
                    }

                    MetadataEntry me = (MetadataEntry) valueReader.run(jdomDocument);
                    metadataEntries.add(me);
                    if (me.getStatus() != CheckStatus.SUCCESS) {
                        log("Couldn't retrieve metadata for setValue-elemenent: '" + valueReader.getName() + "' failed! the error message is "
                                + valueReader.getCode(), LogType.ERROR);
                    }
                }
            } catch (IOException | JDOMException | InterruptedException e) {
                log("A check failed because of an exception. error message: " + e.getMessage(), LogType.ERROR);
                log.error("FileValidationPlugin: A check failed because of an Exception", e);
                report.setErrorMessage("Error running tool or reading report file");
                report.setMetadataEntries(metadataEntries);
                return report;
            }
        }
        report.setMetadataEntries(metadataEntries);
        return report;
    }

    /**
     * runs Checks on the givven file and retruns a report. If the targetlevel was reached the reachedTargetLevel Attribute of the report will be
     * true.
     * 
     * @param targetLevel level which shall be reached
     * @param pathToFile path to the file that shall be checked
     * @return Report-Object
     */
    public Report runChecks(int targetLevel, Path pathToFile) {
        log.debug("FileValidationPlugin: Starting validation of {} target level is {}", pathToFile.getFileName().toString(), targetLevel);
        int reachedLevel = -1;
        int endCheckOnLevel = (runAllChecks) ? ingestLevelChecks.size() - 1 : targetLevel;
        if (targetLevel > ingestLevelChecks.size() - 1) {
            endCheckOnLevel = ingestLevelChecks.size() - 1;
        }
        String fileName = pathToFile.getFileName().toString();
        SAXBuilder jdomBuilder = new SAXBuilder();
        jdomBuilder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        jdomBuilder.setFeature("http://xml.org/sax/features/external-general-entities", false);
        jdomBuilder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        HashMap<String, Document> jdomDocumentsByTool = new HashMap<>();
        List<ReportEntry> reportEntries = new ArrayList<>();
        groupChecksByDependsOn();

        for (int level = 0; level <= endCheckOnLevel; level++) {
            List<Check> checks = ingestLevelChecks.get(level);
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
                        updateDependencies(check.getName());
                        if (groupFailed(check.getGroup(), checksGroupedByGroup)) {
                            Report reportOnAbort = new Report(reachedLevel, check.getCode(), fileName, reportEntries);
                            log("Checks in group failed '" + check.getName() + "' failed! The error message is " + check.getCode(), LogType.ERROR);
                            log.debug("FileValidationPlugin: Checks in group failed '" + check.getName() + "' failed! The error message is "
                                    + check.getCode());
                            reportOnAbort.setReachedTargetLevel(reachedLevel >= targetLevel);
                            return reportOnAbort;
                        } else {
                            log("Check '" + check.getName() + "' failed! The error message is " + check.getCode(), LogType.DEBUG);
                            log.debug("FileValidationPlugin: Check failed '" + check.getName() + "' failed! The error message is " + check.getCode());
                        }
                    }
                }
                reachedLevel = level;

            } catch (IOException | JDOMException | InterruptedException e) {
                log("A check failed because of an exception. ErrorMessage: " + e.getMessage(), LogType.ERROR);
                log.error("FileValidationPlugin: A check failed because of an exception", e);
                return new Report(reachedLevel, "Error running tool or reading report file", fileName, reportEntries);
            }
        }
        Report report = new Report(reachedLevel, null, fileName, reportEntries);
        if (reachedLevel >= targetLevel) {
            report.setReachedTargetLevel(true);
            log.debug("FileValidationPlugin: Validation of file {} was successful reached level is {} target level: {} ",
                    pathToFile.getFileName().toString(), reachedLevel, targetLevel);
        }
        return addReaderReport(endCheckOnLevel, pathToFile, report, jdomDocumentsByTool);
    }

    /**
     * @param group group which shall be tested
     * @param checksGroupedByGroup HashMap with grouped Checks of the current level
     * @return true if ALL checks have one of the following states ERROR, PREQUISITEFAILED, FAILED function also returns true if the given group was
     *         null or if no group with the given name could be found in the provided HashMap.
     */
    private boolean groupFailed(String group, HashMap<String, List<Check>> checksGroupedByGroup) {
        if (group == null) {
            return true;
        }
        List<Check> checks = checksGroupedByGroup.get(group);
        if (checks == null || checks.size() == 1) {
            return true;
        }
        return checks.stream()
                .allMatch(check -> check.getStatus() == CheckStatus.ERROR || check.getStatus() == CheckStatus.PREQUISITEFAILED
                        || check.getStatus() == CheckStatus.FAILED);
    }

    public List<Report> runChecks(int targetLevel) throws IOException, InterruptedException {
        List<Report> reports = new ArrayList<>();
        for (Path pdfFile : this.pdfsInFolder) {
            reports.add(runChecks(targetLevel, pdfFile));
        }

        if (this.pdfsInFolder.isEmpty()) {
            log("ERROR: There are no files in the configured inputFolder: " + this.inputFolder, LogType.ERROR);
            log.error("FileValidationPlugin: ERROR: There are no files in the configured inputFolder: {}", this.inputFolder);
        }
        return reports;
    }
}
