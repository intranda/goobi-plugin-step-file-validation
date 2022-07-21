package de.intranda.goobi.plugins.Reporting;

import java.util.List;

import de.intranda.goobi.plugins.Validation.Check;
import de.intranda.goobi.plugins.Validation.CheckStatus;
import de.intranda.goobi.plugins.Validation.ValueReader;
import lombok.Getter;
import lombok.Setter;

public class MetadataEntry extends ReportEntry {
	@Getter @Setter
	private String processProperty;
	@Getter @Setter
	private String mets;

	public MetadataEntry(Check valueReader) {
		super(valueReader);
		if (valueReader instanceof ValueReader) {
			ValueReader vr = (ValueReader)valueReader;
			this.message = (status != CheckStatus.SUCCESS) ? valueReader.getCode() : "Value retrieved!";
			this.processProperty = vr.getProcessProperty();
			this.mets = vr.getMets();
		}
	}
	//Needed for deep copy
	public MetadataEntry(MetadataEntry entry) {
		super(entry);
		this.processProperty = entry.getProcessProperty();
		this.mets = entry.getMets();
	}

}