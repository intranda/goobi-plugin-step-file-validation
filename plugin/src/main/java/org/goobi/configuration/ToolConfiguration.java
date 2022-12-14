package org.goobi.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ToolConfiguration {
    private String name;
    private String cmd;
    private boolean stdout;
    private String xmlNamespace;
}
