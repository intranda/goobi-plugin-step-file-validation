package de.intranda.goobi.plugins.Logging;

import org.goobi.production.enums.LogType;

@FunctionalInterface
public interface LoggerInterface {
	
	abstract void message(String msg, LogType severity);
}
