package de.intranda.goobi.plugins.Reporting;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class Report {
	@Getter
	private int level;
	@Getter @Setter
	private boolean ReachedTargetLevel=true;
	private List<ReportEntry> reportEntries;
	@Getter @Setter
	private List<MetadataEntry> metadataEntries = new ArrayList<>();
	@Getter @Setter
	private String errorMessage;
	@Getter
	private String fileName;
	
	public Report (int level, String errorMessage,String fileName, List<ReportEntry> reportEntries) {
		this.fileName = fileName;
		this.errorMessage = errorMessage;
		this.level = level;
		this.reportEntries= reportEntries;
	}
	
}
