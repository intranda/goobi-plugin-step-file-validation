package de.intranda.goobi.plugins.logging;

import org.goobi.logging.LoggerInterface;
import org.goobi.production.enums.LogType;

public class ConsoleLogger implements LoggerInterface {

    @Override
    public void message(String msg, LogType severity) {
        System.out.println(severity + ": " + msg);
    }

}
