package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.production.enums.GoobiScriptResultType;
import org.goobi.production.enums.LogType;

import de.intranda.goobi.plugins.Logging.LoggerInterface;
import de.sub.goobi.persistence.managers.PropertyManager;

public class MetadataWriter {
	private List<LoggerInterface> loggers = new ArrayList<>();
	Process process;

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
	
//	public void writeMetadata(Report report) {
//		Processproperty property = null;
//        for (Processproperty pp : p.getEigenschaften()) {
//            if (pp.getTitel().equals(propertyName)) {
//                property = pp;
//            	pp.setWert(value);
//                
//                break;
//            }
//        }
//        if (property == null) {
//        	Processproperty pp = new Processproperty();
//            pp.setTitel(propertyName);
//            pp.setWert(value);
//            pp.setProzess(p);
//        }
//        PropertyManager.saveProcessProperty(pp);
//
//	}
	
}
