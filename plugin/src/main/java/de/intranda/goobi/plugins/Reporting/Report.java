package de.intranda.goobi.plugins.Reporting;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
@Data
public class Report {
	int level;
	List<ReportEntry> entries;
	String errorMessage;
	String fileName;
	
	public Report (int level, String errorMessage,String fileName, List<ReportEntry> entries ) {
		this.fileName = fileName;
		this.errorMessage = errorMessage;
		this.level = level;
		this.entries= entries;
	}
}
