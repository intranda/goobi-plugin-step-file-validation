package de.intranda.goobi.plugins.Validation;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import de.intranda.goobi.plugins.Reporting.ReportEntry;
import lombok.Getter;
import lombok.Setter;

public class Check {
	@Getter
	protected String name;
	@Getter
	protected String dependsOn;
	@Getter @Setter
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
			xpath = XPathFactory.instance().compile(xpathSelector);
		}else {
			xpath = XPathFactory.instance().compile(xpathSelector,Filters.fpassthrough(),null,namespace);
		}
		this.regEx = regEx;
	}
	
	public ReportEntry run(Document doc) {		
		Object value = xpath.evaluateFirst(doc);
		
		//no regex triggers check for existence (null)
		if (regEx==null) {
			if (value ==null) {
				this.status = CheckStatus.FAILED;
				return new ReportEntry(this);
			}else {
				this.status = CheckStatus.SUCCESS;
				return new ReportEntry(this);
			}
		}
		
		if (value instanceof Element) {
			value = ((Element) value).getTextTrim();
		} else if (value instanceof Attribute) {
			value = ((Attribute) value).getValue();
		} else if (value instanceof Text) {
			value = ((Text) value).getText();
		} else if (value != null && !(value instanceof String)) {
			value = value.toString();
		}
		
		if (value != null && value instanceof String && ((String) value).matches(this.regEx)) {
			this.status=CheckStatus.SUCCESS;
			this.value = (String)value;
		}else {
			this.status=CheckStatus.FAILED;
		}
		return new ReportEntry(this);
		
	}
}