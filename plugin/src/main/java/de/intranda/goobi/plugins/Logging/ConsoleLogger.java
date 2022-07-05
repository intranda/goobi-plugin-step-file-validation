package de.intranda.goobi.plugins.Logging;

import org.goobi.production.enums.LogType;

public class ConsoleLogger implements LoggerInterface {

	@Override
	public void message(String msg, LogType severity) {
		System.out.println(severity + ": " + msg);

	}

}
