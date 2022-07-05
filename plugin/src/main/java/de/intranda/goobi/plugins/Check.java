package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import lombok.Getter;

public class Check {
	@Getter
	private String name;
	@Getter
	private String tool;
	@Getter
	private String code;
	@Getter
	private String xpathSelector;
	@Getter
	private XPathExpression xpath;
	@Getter
	private String regEx;

	public Check(String name, String tool, String code, String xpathSelector, String regEx, Namespace namespace) {

		this.name = name;
		this.tool = tool;
		this.code = code;
		this.xpathSelector = xpathSelector;
		if (namespace == null) {
			xpath = XPathFactory.instance().compile(xpathSelector);
		}else {
			xpath = XPathFactory.instance().compile(xpathSelector,Filters.fpassthrough(),null,namespace);
		}
		// TODO validate regEx
		this.regEx = regEx;
	}

	public ReportEntry check(Document doc) {		
		Object value = xpath.evaluateFirst(doc);
		if (regEx==null) {
			if (value ==null) {
				return new ReportEntry(this,value,ReportEntryStatus.FAILED);
			}else {
				return new ReportEntry(this,value,ReportEntryStatus.SUCCESS);
			}
		}
		if (value instanceof Element) {
			value = ((Element) value).getTextTrim();
		} else if (value instanceof Attribute) {
			value = ((Attribute) value).getValue();
		} else if (value instanceof Text) {
			value = ((Text) value).getText();
		} else if (!(value instanceof String)) {
			value = value.toString();
		}
		ReportEntryStatus re = ReportEntryStatus.ERROR;
		if (value != null && value instanceof String && ((String) value).matches(this.regEx)) {
			re = ReportEntryStatus.SUCCESS;
		}else
			re= ReportEntryStatus.FAILED;
		return new ReportEntry(this,value,re);
		
	}
}