package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

import java.util.HashMap;
import java.util.List;

import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.intranda.goobi.plugins.Logging.LoggerInterface;
import de.intranda.goobi.plugins.Logging.ProcessLogger;
import de.intranda.goobi.plugins.Reporting.Report;
import de.intranda.goobi.plugins.Reporting.ReportEntry;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.ProcessManager;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class PdfValidationStepPlugin implements IStepPluginVersion2 {
    
    @Getter
    private String title = "intranda_step_pdf_validation";
    @Getter
    private Step step;
    @Getter
    private String value;
    private LoggerInterface logger;
    private String returnPath;
    private List<List<Check>> levels; 
    private HashMap<String,ToolConfiguration> tools;
    private Process process;
    private ConfigurationParser cParser;
    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;
        this.process= ProcessManager.getProcessById(step.getProcessId());
        this.logger = new ProcessLogger(process);
        
        cParser = new ConfigurationParser(title,step);
        
        this.tools = cParser.getToolConfigurations();
        this.levels = cParser.getIngestLevels();
        
        if (tools.size()>0&&levels.size()>0) {
        	log.info("PdfValidation step plugin initialized"); 
        }else {
        	log.info("Error initializing Plugin");
        	 this.logger.message("Error reading Configuration File",LogType.DEBUG);
        }      
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return "/uii/plugin_step_pdf_validation.xhtml";
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String cancel() {
        return "/uii" + returnPath;
    }

    @Override
    public String finish() {
        return "/uii" + returnPath;
    }
    
    @Override
    public int getInterfaceVersion() {
        return 0;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null;
    }
    
    @Override
    public boolean execute() {
        PluginReturnValue ret = run();
        return ret != PluginReturnValue.ERROR;
    }
    
    public static Report validateFile(Path path) {
    	return validateFile( path, "*");
   }
    
    public static Report validateFile(Path path, String institution) {
    	ConfigurationParser confParser = new ConfigurationParser("intranda_step_pdf_validation",institution);
    	HashMap<String,ToolConfiguration> toolConfigurations = confParser.getToolConfigurations();
    	List<List<Check>> levelsWithChecks = confParser.getIngestLevels();
    	Path outputPath = Paths.get(confParser.getOutputFolder());
    	Report report;
    	//TODO more Checks for the Path maybe with Filter...
    	if (StorageProvider.getInstance().isFileExists(path)) {
        	CheckManager CManager= new CheckManager(toolConfigurations, levelsWithChecks, outputPath);
    		report =CManager.runChecks(confParser.getTargetLevel(),path);
    	}else {
    		report = new Report(-1,"The file could not be found!",path.getFileName().toString(), new ArrayList<ReportEntry>());
    	}

    	return report;
   }

    @Override
    public PluginReturnValue run() {
        boolean successful = true;
        this.process= ProcessManager.getProcessById(step.getProcessId());
        try {
			CheckManager CManager= new CheckManager(tools, levels, this.process, cParser.getFileFiler());
			CManager.addLogger(this.logger);
			List<Report> reports =CManager.runChecks(cParser.getTargetLevel());
			if (reports.isEmpty()) {
				this.logger.message("ERROR: No Report was created!",LogType.INFO);
			}
			for (Report report : reports) {
				if (report.getLevel()<cParser.getTargetLevel()) {
					this.logger.message("ERROR: The File "+report.getFileName()+" did not reach the required target level!",LogType.ERROR);
					successful= false;
				}else {
					this.logger.message("The File "+report.getFileName()+" reached the required target level!",LogType.INFO);
				}
			}
		} catch (IOException | InterruptedException | SwapException | DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        log.info("PdfValidation step plugin executed");
        if (!successful) {
            return PluginReturnValue.ERROR;
        }
        return PluginReturnValue.FINISH;
    } 
}


