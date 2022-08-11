package org.goobi.files;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.goobi.configuration.ConfigurationParser;
import org.goobi.configuration.ToolConfiguration;
import org.goobi.reporting.Report;
import org.goobi.validation.Check;
import org.goobi.validation.CheckManager;
import org.goobi.validation.ValueReader;

import de.sub.goobi.helper.StorageProvider;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FileValidator {

    private FileValidator() {
    }

    /**
     * This function will create a folder with reports in the folder where the file is located It will also return a report-object that contains lists
     * with entries of report and metadata entries.
     * 
     * @param path path to the file
     * @param institution name of the institution to select the right profile.
     * @return Report with ReportEntries, MetadataEntries, reachedlevel
     */
    public static Report validateFile(Path path, String institution) {
        ConfigurationParser confParser = null;
        Report report = null;
        try {
            confParser = new ConfigurationParser("intranda_step_file_validation", institution);
        } catch (IllegalArgumentException ex) {
            log.error(ex);
            return null;
        }
        String fileName = FileUtils.removeFileExtension(path.getFileName().toString());
        HashMap<String, ToolConfiguration> toolConfigurations = confParser.getToolConfigurations();
        List<List<Check>> levelWithChecks = confParser.getIngestLevelChecks();
        List<List<ValueReader>> levelWithReaders = confParser.getIngestLevelReader();
        Path outputPath = Paths.get(path.getParent().toString(), fileName);

        // TODO more Checks for the Path maybe with Filter...
        if (StorageProvider.getInstance().isFileExists(path)) {
            CheckManager cManager = new CheckManager(toolConfigurations, levelWithChecks, levelWithReaders, outputPath);
            report = cManager.runChecks(confParser.getTargetLevel(), path);
        } else {
            report = new Report(-1, "The file could not be found!", path.getFileName().toString(), new ArrayList<>());
        }
        return report;
    }

}
