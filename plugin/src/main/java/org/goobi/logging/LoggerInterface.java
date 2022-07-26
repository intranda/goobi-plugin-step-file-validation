package org.goobi.logging;

import org.goobi.production.enums.LogType;

@FunctionalInterface
public interface LoggerInterface {

    /**
     * sends a message
     * 
     * @param msg message
     * @param severity how important is the message
     */
    abstract void message(String msg, LogType severity);
}
