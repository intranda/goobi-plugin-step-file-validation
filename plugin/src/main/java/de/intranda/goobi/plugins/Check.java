package de.intranda.goobi.plugins;

import org.jdom2.Document;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class Check {
    
	private String name; 
	private String tool;
    private String code;
    private String xpathSelector;
    private String regEx;
    
//    public Check (String name, String tool, String code, String xpathSelector, String regEx) {
//    	XPathFactory xFactory = XPathFactory.newInstance();
//    	this.name = name;
//    	this.tool = tool;
//    	//TODO validate xpath
//    	
//    	Xpath xpath = xFactory.newX
//    	this.xpathSelector = xpathSelector;
//    	//TODO validate regEx
//    	this.regEx = regEx;
//    }
//    
//    public boolean check(Document doc) {
//        Object value = xpath.evaluateFirst(doc);
//        if (value instanceof Element) {
//            value = ((Element) value).getTextTrim();
//        } else if (value instanceof Attribute) {
//            value = ((Attribute) value).getValue();
//        } else if (value instanceof Text) {
//            value = ((Text) value).getText();
//        } else if (!(value instanceof String)) {
//            value = value.toString();
//        }
//        return this.expectedValue.equals(value) || (value != null && value instanceof String && ((String) value).matches(this.expectedValue));
//        //        return this.expectedValue.equals(value);
//    }
} 