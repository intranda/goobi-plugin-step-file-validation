package de.intranda.goobi.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.production.enums.LogType;

import de.intranda.goobi.plugins.Logging.LoggerInterface;
import de.intranda.goobi.plugins.Reporting.MetadataEntry;
import de.intranda.goobi.plugins.Reporting.Report;
import de.sub.goobi.helper.exceptions.DAOException;
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

	public void addLogger(LoggerInterface logger) {
		loggers.add(logger);
	}

	public void log(String message, LogType type) {
		for (LoggerInterface logger : loggers) {
			logger.message(message, type);
		}
	}
	
	public void writeReportresults(List<Report> reports) throws MetadataWriterException{
		if (reports.size()==1) {
			writeMetadata(reports.get(0).getMetadataEntries());
		}else {
			HashMap<String,MetadataEntry> report = generateSummaryReport(reports);
		}
	}
	

	private   HashMap<String, MetadataEntry> generateSummaryReport(List<Report> reports) {
		List <HashMap<String, MetadataEntry>> mappedEntriesList = new ArrayList<>();
		
		//create HashMap for easy retrival
		for (int i=0;i<reports.size();i++) {
			List<MetadataEntry> entries = reports.get(i).getMetadataEntries();
			HashMap<String, MetadataEntry> mappedEntries = new HashMap<>();
			for (MetadataEntry entry: entries) {
				mappedEntries.put(entry.getCheckName(),entry);
			}
			mappedEntriesList.add(mappedEntries);
		}
		
		HashMap<String, MetadataEntry>resultMap = new HashMap<>();
		for (int i =0; i < mappedEntriesList.size(); i++) {
			Set<String> resultMapKeys = resultMap.keySet();
			Set<String> entryMapKey = mappedEntriesList.get(i).keySet();
			
			// add Elements to HashMap or update existing Elements
			for(MetadataEntry entry: mappedEntriesList.get(i).values()) {
				MetadataEntry currentEntry = resultMap.get(entry.getCheckName());
				if (currentEntry==null) {
					resultMap.put(entry.getCheckName(),new MetadataEntry(entry));
				}
				else {
					String message = currentEntry.getMessage()+"; "+entry.getMessage();
					currentEntry.setMessage(message);
					String value = currentEntry.getValue()+"; "+entry.getValue();
					currentEntry.setMessage(value);
				}
				
			}
			// add Semicolon to Elements that were not edited this time
			resultMapKeys.removeAll(entryMapKey);
			for (String key: resultMapKeys) {
				MetadataEntry entry = resultMap.get(entryMapKey);
				String message = entry.getMessage()+"; ";
				entry.setMessage(message);
				String value = entry.getValue()+"; ";
				entry.setMessage(value);
			}
		}
		return resultMap;
	}
	


	public void writeMetadata(List<MetadataEntry> entries) throws MetadataWriterException {
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
			log("An Empty Value was added for the Process Property: " + entry.getProcessProperty(), LogType.DEBUG);
		} else {
			log("The Value: " + entry.getValue() + " was added for the Process Property: " + entry.getProcessProperty(),
					LogType.INFO);
		}
		
	}

	private void writeToTopstruct(MetadataEntry entry) throws MetadataWriterException {
		Fileformat ff;
		DocStruct ds = null;

		try {
			ff = this.process.readMetadataFile();
			// first get the top element
			ds = ff.getDigitalDocument().getLogicalDocStruct();
			DocStruct physical = ff.getDigitalDocument().getPhysicalDocStruct();

			// find topstruct to add metadata
			if (ds.getType().isAnchor()) {
				ds.getAllChildren().get(0);
			}

			process.writeMetadataFile(ff);
		} catch (ReadException | PreferencesException | WriteException | IOException | InterruptedException
				| SwapException | DAOException e) {
			throw new MetadataWriterException("Error opening Metadatfile or getting logical Docstruct",e);
		}

		addMetadata(ds, entry.getMets(), entry.getValue());

	}

	private void addMetadata(DocStruct ds, String metsField, String value) {
		if (StringUtils.isEmpty(value)) {
			log("Error: value that should be save in metsField: " + metsField + " was empty. No entry was created!",
					LogType.ERROR);
		}
		Prefs prefs = process.getRegelsatz().getPreferences();
		Metadata metadatata;
		try {
			metadatata = new Metadata(prefs.getMetadataTypeByName(metsField));
			metadatata.setValue(value);
			ds.addMetadata(metadatata);
		} catch (MetadataTypeNotAllowedException e) {
			log("Error: metadata type " + metsField + " is not allowed for the TopStruct. Please update ruleset.",
					LogType.ERROR);
		}
	}

}