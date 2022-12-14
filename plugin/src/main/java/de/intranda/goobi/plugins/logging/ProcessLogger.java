package de.intranda.goobi.plugins.logging;

import org.goobi.beans.Process;
import org.goobi.logging.LoggerInterface;
import org.goobi.production.enums.LogType;

import de.sub.goobi.helper.Helper;

public class ProcessLogger implements LoggerInterface {
    Process process;

    public ProcessLogger(Process process) {
        this.process = process;
    }

    @Override
    public void message(String msg, LogType severity) {
        Helper.addMessageToProcessJournal(process.getId(), severity, "FileValidationPlugin: " + msg);
    }
}
