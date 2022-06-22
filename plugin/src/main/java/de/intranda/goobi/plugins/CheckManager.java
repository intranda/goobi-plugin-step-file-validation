package de.intranda.goobi.plugins;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CheckManager {

	private List<ToolConfiguration> toolConfigurations;
	private List<List<Check>> ingestLevels;
	private String jhoveConfigurationFile;
	private String verpdfConfigurationFile;

	// mayber refactor later

	public CheckManager() {
		// readConfiguration
	}

	public CheckManager(List<ToolConfiguration> toolsConfigurations, List<List<Check>> ingestLevels) {
		this.toolConfigurations = toolsConfigurations;
		this.ingestLevels = ingestLevels;
	}

	public boolean runChecks(int targetLevel, Path file) {
		int reachedLevel = -1;
		boolean success = true;
		for (int i = 0; i < ingestLevels.size(); i++) {
			for (Check check : ingestLevels.get(i)) {
				if (!runcheck(check, file)) {
					success = false;
				}
			}
			if (success) {
				reachedLevel = i;
			}
		}
		return true;
	}

	private boolean runcheck(Check check, Path file) {
		//TODO generate outputfile 
		
		//run tests against output
		//maybe add switch that generates output even if it's not needed here
		
		switch (check.getTool()) {
		case "verapdf":
			break;
		case "jhove":
			break;
		}
		
		// TODO Auto-generated method stub
		return false;
	}

	// maybe this should be done earlier
	private boolean checkToolConfiguration() {
		boolean success = true;
		for (ToolConfiguration tc : toolConfigurations) {
			switch (tc.getName()) {
			case "verapdf":
				if (Files.exists(Paths.get(tc.getPath()))) {
					this.verpdfConfigurationFile = tc.getPath();
				} else {
					success = false;
				}

				break;
			case "jhove":
				if (Files.exists(Paths.get(tc.getPath()))) {
					this.jhoveConfigurationFile = tc.getPath();
				} else {
					success = false;
				}
				break;
			}
		}
		return success;
	}

}
