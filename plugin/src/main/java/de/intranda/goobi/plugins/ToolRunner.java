package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.TimeUnit;

public class ToolRunner {
	private ToolConfiguration toolConfiguration;
	private Path outputPath;
	private Process ToolProcess;
	private StreamConsumer streamConsumer;

	public ToolRunner(ToolConfiguration tc, Path outputPath) {
		this.toolConfiguration = tc;
		this.outputPath = outputPath;
	}

	public SimpleEntry<String, String> runTool(Path pdfFile) throws IOException, InterruptedException {
		String inputName = pdfFile.getFileName().toString();
		String outputName = inputName.substring(0, inputName.lastIndexOf('.')) + "_" + this.toolConfiguration.getName()
				+ ".xml";
		Path fOutputPath = outputPath.resolve(outputName);

		// replace variables in String or use environment variables
		String cmd = this.toolConfiguration.getCmd().replace("{pv.outputFile}", fOutputPath.toString())
				.replace("{pv.inputFile}", pdfFile.toString());

		if (toolConfiguration.isStdout()) {
			// --> reroute stdout to file
		} else {
			ProcessBuilder builder = new ProcessBuilder("notepad.exe");
			ToolProcess = builder.start();
			ToolProcess.waitFor(360, TimeUnit.SECONDS);
		}
		return new SimpleEntry<>(pdfFile.toString(), fOutputPath.toString());
	}

}
