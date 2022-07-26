package org.goobi.validation;

import org.goobi.reporting.ReportEntry;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import lombok.Getter;
import lombok.Setter;

public class Check {
    @Getter
    protected String name;
    @Getter
    protected String dependsOn;
    @Getter
    @Setter
    protected CheckStatus status;
    @Getter
    protected String group;
    @Getter
    protected String tool;
    @Getter
    protected String code;
    @Getter
    protected XPathExpression xpath;
    @Getter
    private String regEx;
    @Getter
    protected String value;

    public Check(String name, String dependsOn, String group, String tool, String code, String xpathSelector, String regEx, Namespace namespace) {

        this.name = name;
        this.dependsOn = dependsOn;
        this.group = group;
        this.status = CheckStatus.NEW;
        this.tool = tool;
        this.code = code;
        if (namespace == null) {
            this.xpath = XPathFactory.instance().compile(xpathSelector);
        } else {
            this.xpath = XPathFactory.instance().compile(xpathSelector, Filters.fpassthrough(), null, namespace);
        }
        this.regEx = regEx;
    }

    /**
     * runs the check on a given JDOM Document. If the node exists an no regex is provided the status of the check will be switched to success. If a
     * regex is provided the status of the check will only switch to success if the value matches the pattern
     * 
     * @param doc
     * @return ReportEntry with relevant data of the check process
     */
    public ReportEntry run(Document doc) {
        Object val = xpath.evaluateFirst(doc);

        //no regex triggers check for existence (null)
        if (regEx == null) {
            if (val == null) {
                this.status = CheckStatus.FAILED;
                return new ReportEntry(this);
            } else {
                this.status = CheckStatus.SUCCESS;
                return new ReportEntry(this);
            }
        }

        if (val instanceof Element) {
            val = ((Element) val).getTextTrim();
        } else if (val instanceof Attribute) {
            val = ((Attribute) val).getValue();
        } else if (val instanceof Text) {
            val = ((Text) val).getText();
        } else if (val != null && !(val instanceof String)) {
            val = val.toString();
        }

        if (val instanceof String && ((String) val).matches(this.regEx)) {
            this.status = CheckStatus.SUCCESS;
            this.value = (String) val;
        } else {
            this.status = CheckStatus.FAILED;
        }
        return new ReportEntry(this);

    }
}