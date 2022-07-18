package de.intranda.goobi.plugins;

import org.goobi.beans.Process;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;

import de.intranda.goobi.plugins.Reporting.ReportEntry;
import de.intranda.goobi.plugins.Reporting.ReportEntryStatus;

public class ValueReader extends Check {
	private String processProperty;
	private String metadata;
	private String value;

	public ValueReader(String name, String dependsOn, String group, String tool, String code, String xpathSelector,
			String regEx, Namespace namespace, String processProperty, String metadata) {
		super(name, dependsOn, group, tool, code, xpathSelector, regEx, namespace);
		this.metadata = metadata;
	}

	public ReportEntry run(Document doc) {
		Object value = xpath.evaluateFirst(doc);

		if (value instanceof Element) {
			value = ((Element) value).getTextTrim();
		} else if (value instanceof Attribute) {
			value = ((Attribute) value).getValue();
		} else if (value instanceof Text) {
			value = ((Text) value).getText();
		} else if (value != null && !(value instanceof String)) {
			value = value.toString();
		}

		ReportEntryStatus re = ReportEntryStatus.ERROR;
		if (value != null && value instanceof String) {
			this.status = CheckStatus.SUCCESS;
			this.value = (String) value;
		} else {
			this.status = CheckStatus.FAILED;
		}
		return new ReportEntry(this, value);
	}
	
	public void save(Process process) {
		if (this.metadata!=null) {
			
		}	
	}
}
