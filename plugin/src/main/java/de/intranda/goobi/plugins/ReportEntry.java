package de.intranda.goobi.plugins;

import lombok.Getter;
import lombok.Setter;

public class ReportEntry {
	@Getter @Setter
	private Check check;
	@Getter @Setter
	private String value;
	@Getter @Setter
	private String status;
	
	
}
