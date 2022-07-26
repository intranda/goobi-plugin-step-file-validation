package de.intranda.goobi.plugins.logging;

import org.goobi.beans.Process;
import org.goobi.production.enums.LogType;

import de.sub.goobi.helper.Helper;

public class ProcessLogger implements LoggerInterface {
    Process process;

    public ProcessLogger(Process process) {
        this.process = process;
    }

    @Override
    public void message(String msg, LogType severity) {
        Helper.addMessageToProcessLog(process.getId(), severity, msg);
    }
}
