package de.intranda.goobi.plugins;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.TimeUnit;

import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.StorageProviderInterface;

public class ToolRunner {
	private ToolConfiguration toolConfiguration;
	private Path outputPath;
	private Process ToolProcess;

	public ToolRunner(ToolConfiguration tc, Path outputPath) {
		this.toolConfiguration = tc;
		this.outputPath = outputPath;
	}

	/**
	 * executes the tool for a given file and creates a report
	 * 
	 * @param pdfFile path to the pdf-file
	 * @return returns a simpleEntry with the input and the output file path
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public SimpleEntry<String, String> runTool(Path pdfFile) throws IOException, InterruptedException {
		String inputName = pdfFile.getFileName().toString();
		String outputName = inputName.substring(0, inputName.lastIndexOf('.')) + "_" + this.toolConfiguration.getName()
				+ ".xml";
		
		//create OutputFolder if it does not exist
		StorageProviderInterface SPI = StorageProvider.getInstance();
		if (!SPI.isFileExists(outputPath))
			SPI.createDirectories(outputPath);
		Path fOutputPath = outputPath.resolve(outputName);

		// replace variables in String or use environment variables
		String cmd = this.toolConfiguration.getCmd().replace("{pv.outputFile}", fOutputPath.toString())
				.replace("{pv.inputFile}", pdfFile.toString());

		if (toolConfiguration.isStdout()) {
			ToolProcess = Runtime.getRuntime().exec(cmd);
			String line;
			BufferedReader input = new BufferedReader(new InputStreamReader(ToolProcess.getInputStream()));
			File reportfile = new File(fOutputPath.toString());
			BufferedWriter writer = Files.newBufferedWriter(fOutputPath, StandardCharsets.UTF_8);
			  while ((line = input.readLine()) != null) {
			    writer.write(line);
			    writer.newLine();
			  }
			  writer.close();
			  input.close();
			// --> reroute stdout to file
		} else {
			// ProcessBuilder builder = new ProcessBuilder(cmd);
			// ToolProcess = builder.start();
			ToolProcess = Runtime.getRuntime().exec(cmd);
			ToolProcess.waitFor(360, TimeUnit.SECONDS);
		}
		return new SimpleEntry<>(pdfFile.toString(), fOutputPath.toString());
	}

}
