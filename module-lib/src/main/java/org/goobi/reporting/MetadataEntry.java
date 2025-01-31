package org.goobi.reporting;

import org.goobi.validation.Check;
import org.goobi.validation.CheckStatus;
import org.goobi.validation.ValueReader;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@XmlRootElement(name = "metadataEntry")
@XmlAccessorType(XmlAccessType.FIELD)
public class MetadataEntry extends ReportEntry {
    @Getter
    @Setter
    private String processProperty;
    @Getter
    @Setter
    private String mets;

    public MetadataEntry(Check valueReader) {
        super(valueReader);
        if (valueReader instanceof ValueReader vr) {
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
