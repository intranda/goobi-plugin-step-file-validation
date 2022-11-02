package de.intranda.goobi.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.logging.LoggerInterface;
import org.goobi.production.enums.LogType;
import org.goobi.reporting.MetadataEntry;
import org.goobi.reporting.Report;

import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.PropertyManager;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Prefs;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.WriteException;

public class MetadataWriter {
    private List<LoggerInterface> loggers = new ArrayList<>();
    private Process process;

    public MetadataWriter(Process process) {
        this.process = process;
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

    /**
     * Writes the Values in the setValue Elements into the specified Metadata fields of the TopStruct Element or into the specified process property.
     * If more than one report is provieded alle values are crammed into one field
     * 
     * @param reports
     * @throws MetadataWriterException
     */
    public void writeReportResults(List<Report> reports) throws MetadataWriterException {
        if (reports.size() == 1) {
            writeMetadata(reports.get(0).getMetadataEntries());
        } else {
            HashMap<String, MetadataEntry> reportMap = generateSummaryReport(reports);
            if (reportMap != null) {
                writeMetadata(new ArrayList<>(reportMap.values()));
            }
        }
    }

    private HashMap<String, MetadataEntry> generateSummaryReport(List<Report> reports) {
        List<HashMap<String, MetadataEntry>> mappedEntriesList = new ArrayList<>();

        //create HashMap for easy retrival
        for (int i = 0; i < reports.size(); i++) {
            List<MetadataEntry> entries = reports.get(i).getMetadataEntries();
            HashMap<String, MetadataEntry> mappedEntries = new HashMap<>();
            for (MetadataEntry entry : entries) {
                mappedEntries.put(entry.getName(), entry);
            }
            mappedEntriesList.add(mappedEntries);
        }

        HashMap<String, MetadataEntry> resultMap = new HashMap<>();
        for (int i = 0; i < mappedEntriesList.size(); i++) {
            Set<String> resultMapKeys = resultMap.keySet();
            Set<String> entryMapKey = mappedEntriesList.get(i).keySet();

            // add Elements to HashMap or update existing Elements
            for (MetadataEntry entry : mappedEntriesList.get(i).values()) {
                MetadataEntry currentEntry = resultMap.get(entry.getName());
                if (currentEntry == null) {
                    MetadataEntry newEntry = new MetadataEntry(entry);
                    if (i > 0) {
                        //add ; for each report that was handled before this entry was added
                        StringBuilder sb = new StringBuilder();
                        for (int reportNumber = 0; reportNumber < i; reportNumber++) {
                            sb.append(" ;");
                        }
                        sb.append(newEntry.getValue() + ";");
                        newEntry.setValue(sb.toString());
                    }
                    resultMap.put(entry.getName(), newEntry);
                } else {
                    String message = currentEntry.getMessage() + "; " + entry.getMessage();
                    currentEntry.setMessage(message);
                    String value = currentEntry.getValue() + "; " + entry.getValue();
                    currentEntry.setMessage(value);
                }

            }
            // add Semicolon to Elements that were not edited this time
            resultMapKeys.removeAll(entryMapKey);
            for (String key : resultMapKeys) {
                MetadataEntry entry = resultMap.get(entryMapKey);
                String message = entry.getMessage() + "; ";
                entry.setMessage(message);
                String value = entry.getValue() + "; ";
                entry.setMessage(value);
            }
        }
        return resultMap;
    }

    private void writeMetadata(List<MetadataEntry> entries) throws MetadataWriterException {
        // TODO catch empty entries if needed
        for (MetadataEntry entry : entries) {
            if (StringUtils.isNotBlank(entry.getProcessProperty())) {
                writeProcessProperty(entry);
            }
            if (StringUtils.isNotBlank(entry.getMets())) {
                writeToTopstruct(entry);
            }
        }
    }

    private void writeProcessProperty(MetadataEntry entry) {
        Processproperty property = null;
        for (Processproperty pp : process.getEigenschaften()) {
            if (pp.getTitel().equals(entry.getProcessProperty())) {
                property = pp;
                property.setWert(entry.getValue());
                break;
            }
        }

        if (property == null) {
            property = new Processproperty();
            property.setTitel(entry.getProcessProperty());
            property.setWert(entry.getValue());
            property.setProzess(this.process);
        }

        PropertyManager.saveProcessProperty(property);
        if (StringUtils.isBlank(entry.getValue())) {
            log("An empty value was added for the process property: " + entry.getProcessProperty(), LogType.DEBUG);
        } else {
            log("The value: " + entry.getValue() + " was added for the process property: " + entry.getProcessProperty(), LogType.DEBUG);
        }

    }

    private void writeToTopstruct(MetadataEntry entry) throws MetadataWriterException {
        Fileformat ff;
        DocStruct ds = null;

        try {
            ff = this.process.readMetadataFile();
            // first get the top element
            ds = ff.getDigitalDocument().getLogicalDocStruct();

            // find topstruct to add metadata
            if (ds.getType().isAnchor()) {
                ds.getAllChildren().get(0);
            }

            process.writeMetadataFile(ff);
        } catch (ReadException | PreferencesException | WriteException | IOException | SwapException e) {
            throw new MetadataWriterException("Error opening metadata file or getting logical DocStruct", e);
        }

        addMetadata(ds, entry.getMets(), entry.getValue());

    }

    private void addMetadata(DocStruct ds, String metsField, String value) {
        if (StringUtils.isEmpty(value)) {
            log("Error: value that should be saved in metsField: " + metsField + " was empty. No entry was created!", LogType.ERROR);
        }
        Prefs prefs = process.getRegelsatz().getPreferences();
        Metadata metadatata;
        try {
            metadatata = new Metadata(prefs.getMetadataTypeByName(metsField));
            metadatata.setValue(value);
            ds.addMetadata(metadatata);
        } catch (MetadataTypeNotAllowedException e) {
            log("Error: metadata type " + metsField + " is not allowed for the topStruct. Please update ruleset or the configuration file.",
                    LogType.ERROR);
        }
    }

}
