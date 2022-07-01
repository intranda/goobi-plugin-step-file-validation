package de.intranda.goobi.plugins;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
public class ReportEntry {
	@Getter @Setter
	private String checkName;
	@Getter @Setter
	private String value;
	@Getter @Setter
	private ReportEntryStatus status;
	private String message;

	public ReportEntry (Check check, Object value, ReportEntryStatus res) {
		this.checkName = check.getName();
		this.status = res;
		this.value = (value==null)? "":(String)value;
		this.message = (res!=ReportEntryStatus.SUCCESS)? check.getCode(): "Check passed!";
	}
	
}
