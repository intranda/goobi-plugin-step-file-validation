package de.intranda.goobi.plugins;

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
} 