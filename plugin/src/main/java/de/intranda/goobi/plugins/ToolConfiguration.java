package de.intranda.goobi.plugins;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ToolConfiguration {
	private String name;
	private String path;
}