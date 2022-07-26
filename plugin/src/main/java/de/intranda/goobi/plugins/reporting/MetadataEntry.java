package de.intranda.goobi.plugins.reporting;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import de.intranda.goobi.plugins.validation.Check;
import de.intranda.goobi.plugins.validation.CheckStatus;
import de.intranda.goobi.plugins.validation.ValueReader;
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
        if (valueReader instanceof ValueReader) {
            ValueReader vr = (ValueReader) valueReader;
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
