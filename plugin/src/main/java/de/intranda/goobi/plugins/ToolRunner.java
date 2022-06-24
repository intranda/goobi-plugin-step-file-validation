package de.intranda.goobi.plugins;

import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;

public class ToolRunner {
	private ToolConfiguration toolConfiguration;
	private Path outputPath;
	
	public ToolRunner(ToolConfiguration tc, Path outputPath) {
		this.toolConfiguration = tc;
	}

	public SimpleEntry<String, String> run(Path pdfFile) {
		String inputName = pdfFile.getFileName().toString();
	    String outputName = inputName.substring(0, inputName.lastIndexOf('.'))+ "_" + this.toolConfiguration.getName() + ".xml";
	    Path fOutputPath = outputPath.resolve(outputName);
	    //HashMap<String,List<SimpleEntry<String, String>>> inputOutputList = new HashMap();
	    
		if (toolConfiguration.isStdout()) {
			//--> reroute stdout to file
		}else {
			//--> check if cmd needs variable replacement
		}
		return new SimpleEntry<>(pdfFile.toString(), fOutputPath.toString());
	}
}
