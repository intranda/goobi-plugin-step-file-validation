package org.goobi.reporting;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@XmlRootElement(name = "report")
@XmlAccessorType(XmlAccessType.FIELD)
public class Report {
    @Getter
    private String fileName;
    @Getter
    private int level;
    @Getter
    @Setter
    private String errorMessage;
    @Getter
    @Setter
    private boolean reachedTargetLevel = false;;
    @XmlElementWrapper(name = "reportEntries")
    @XmlElement(name = "reportEntry")
    @Getter
    private List<ReportEntry> reportEntries;
    @XmlElementWrapper(name = "metadataEntries")
    @XmlElement(name = "metadataEntry")
    @Getter
    @Setter
    private List<MetadataEntry> metadataEntries = new ArrayList<>();

    public Report(int level, String errorMessage, String fileName, List<ReportEntry> reportEntries) {
        this.fileName = fileName;
        this.errorMessage = errorMessage;
        this.level = level;
        this.reportEntries = reportEntries;
    }

}
