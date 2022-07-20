package de.intranda.goobi.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.production.enums.GoobiScriptResultType;
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
	List<MetadataEntry> entries;

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

	public void writeMetadata(Report report) {
		writeMetadata(report.getMetadataEntries());
	}

	public void writeMetadata(List<MetadataEntry> entries) {
		this.entries = entries;
		// TODO catch empty entries if needed
		for (MetadataEntry entry : entries) {
			if (StringUtils.isNotBlank(entry.getProcessProperty())) {
				writeProcessProperty(entry);
			}
			if (StringUtils.isNotBlank(entry.getMets())) {
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
		if (StringUtils.isBlank(entry.getValue())) {
			log("An Empty Value was added for the Process Property: " + entry.getProcessProperty(), LogType.DEBUG);
		} else {
			log("The Value: " + entry.getValue() + " was added for the Process Property: " + entry.getProcessProperty(),
					LogType.INFO);
		}
		PropertyManager.saveProcessProperty(property);
	}

	private void writeMetadata(MetadataEntry entry) {
		Fileformat ff;
		DocStruct ds = null;
		try {
			ff = this.process.readMetadataFile();
			// first get the top element
			ds = ff.getDigitalDocument().getLogicalDocStruct();
			DocStruct physical = ff.getDigitalDocument().getPhysicalDocStruct();
		} catch (ReadException | PreferencesException | WriteException | IOException | InterruptedException
				| SwapException | DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// find the right element to adapt
		if (ds.getType().isAnchor()) {
			ds.getAllChildren().get(0);
		}

//		addMetadata(ds, entry.getMets(), entry.getValue());
//		try {
//			process.writeMetadataFile(ff);
//		} catch (WriteException | PreferencesException | IOException | InterruptedException | SwapException
//				| DAOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

//	private void addMetadata(DocStruct ds, String field, String value) {
//		try {
//			Metadata metadatata = new Metadata(prefs.getMetadataTypeByName(field));
//			metadatata.setValue(value);
//			ds.addMetadata(metadatata);
//		} catch (MetadataTypeNotAllowedException e) {
//			log("Metadata not allowed", LogType.ERROR);
//		}
//	}

}
