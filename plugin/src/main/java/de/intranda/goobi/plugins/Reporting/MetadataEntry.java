package de.intranda.goobi.plugins.Reporting;

import java.util.List;

import de.intranda.goobi.plugins.Check;
import de.intranda.goobi.plugins.CheckStatus;
import de.intranda.goobi.plugins.ValueReader;
import lombok.Getter;

public class MetadataEntry extends ReportEntry {
	@Getter
	private String processProperty;
	@Getter
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

}
