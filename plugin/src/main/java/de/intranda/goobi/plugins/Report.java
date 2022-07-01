package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
@Data
public class Report {
	int level;
	List<ReportEntry> entries;
	String errorMessage;
	
	public Report (int level, String errorMessage, List<ReportEntry> entries ) {
		this.errorMessage = errorMessage;
		this.level = level;
		this.entries= entries;
	}
}
