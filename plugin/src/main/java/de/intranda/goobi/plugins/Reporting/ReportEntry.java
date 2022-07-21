package de.intranda.goobi.plugins.Reporting;

import de.intranda.goobi.plugins.Validation.Check;
import de.intranda.goobi.plugins.Validation.CheckStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ReportEntry {
	@Getter @Setter
	protected String checkName;
	@Getter @Setter
	protected String value;
	@Getter @Setter
	protected CheckStatus status;
	@Getter @Setter
	protected String message;
	
	public ReportEntry (Check check) {
		this.checkName = check.getName();
		this.status = check.getStatus();
		this.value = (check.getValue()==null)? "":check.getValue();
		this.message = (status!=CheckStatus.SUCCESS)? check.getCode(): "Check passed!";
	}
	
	public ReportEntry(ReportEntry entry) {
		this.checkName = entry.getCheckName();
		this.value = entry.getValue();
		this.message = entry.getMessage();
	}
	
}
