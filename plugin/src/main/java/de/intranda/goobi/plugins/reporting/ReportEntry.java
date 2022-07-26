package de.intranda.goobi.plugins.reporting;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import de.intranda.goobi.plugins.validation.Check;
import de.intranda.goobi.plugins.validation.CheckStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@XmlRootElement(name = "reportEntry")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReportEntry {
    @Getter
    @Setter
    protected String name;
    @Getter
    @Setter
    protected String value;
    @Getter
    @Setter
    protected CheckStatus status;
    @Getter
    @Setter
    protected String message;

    public ReportEntry(Check check) {
        this.name = check.getName();
        this.status = check.getStatus();
        this.value = (check.getValue() == null) ? "" : check.getValue();
        this.message = (status != CheckStatus.SUCCESS) ? check.getCode() : "Check passed!";
    }

    public ReportEntry(ReportEntry entry) {
        this.name = entry.getName();
        this.value = entry.getValue();
        this.message = entry.getMessage();
    }

}
