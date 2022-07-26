package de.intranda.goobi.plugins.validation;

import org.goobi.beans.Process;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;

import de.intranda.goobi.plugins.reporting.MetadataEntry;
import de.intranda.goobi.plugins.reporting.ReportEntry;
import lombok.Getter;

public class ValueReader extends Check {
    @Getter
    private String processProperty;
    @Getter
    private String mets;

    public ValueReader(String name, String dependsOn, String group, String tool, String code, String xpathSelector, String regEx, Namespace namespace,
            String processProperty, String mets) {
        super(name, dependsOn, group, tool, code, xpathSelector, regEx, namespace);
        this.mets = mets;
        this.processProperty = processProperty;
    }

    @Override
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

        if (value instanceof String) {
            this.status = CheckStatus.SUCCESS;
            this.value = (String) value;
        } else {
            this.status = CheckStatus.FAILED;
        }
        return new MetadataEntry(this);
    }

    public void save(Process process) {
        if (this.mets != null) {
            // do nothing?
        }
    }
}
