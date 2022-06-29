package de.intranda.goobi.plugins;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
public class ReportEntry {
	@Getter @Setter
	private Check check;
	@Getter @Setter
	private String value;
	@Getter @Setter
	private ReportEntryStatus status;

	public ReportEntry (Check check, Object value, ReportEntryStatus res) {
		this.check = check;
		this.status = res;
		this.value = (value==null)? "":(String)value;
	}
	
}
