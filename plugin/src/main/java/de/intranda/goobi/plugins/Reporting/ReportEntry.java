package de.intranda.goobi.plugins.Reporting;

import de.intranda.goobi.plugins.Check;
import de.intranda.goobi.plugins.CheckStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class ReportEntry {
	@Getter @Setter
	private String checkName;
	@Getter @Setter
	private String value;
	@Getter @Setter
	private CheckStatus status;
	@Getter @Setter
	private String message;

	public ReportEntry (Check check, Object value) {
		this.checkName = check.getName();
		this.status = check.getStatus();
		this.value = (value==null)? "":(String)value;
		this.message = (status!=CheckStatus.SUCCESS)? check.getCode(): "Check passed!";
	}
	
}
