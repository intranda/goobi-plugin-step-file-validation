package org.goobi.files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.TimeUnit;

import org.goobi.configuration.ToolConfiguration;

import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.StorageProviderInterface;

public class ToolRunner {
    private ToolConfiguration toolConfiguration;
    private Path outputPath;

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
        String outputName = inputName.substring(0, inputName.lastIndexOf('.')) + "_" + this.toolConfiguration.getName() + ".xml";

        //create OutputFolder if it does not exist
        StorageProviderInterface spi = StorageProvider.getInstance();
        if (!spi.isFileExists(outputPath)) {
            spi.createDirectories(outputPath);
        }
        Path fOutputPath = outputPath.resolve(outputName);

        // replace variables in String or use environment variables
        String cmd = this.toolConfiguration.getCmd().replace("{pv.outputFile}", fOutputPath.toString()).replace("{pv.inputFile}", pdfFile.toString());

        if (toolConfiguration.isStdout()) {
            Process toolProcess = Runtime.getRuntime().exec(cmd);
            String line;
            BufferedReader input = new BufferedReader(new InputStreamReader(toolProcess.getInputStream()));
            try (BufferedWriter writer = Files.newBufferedWriter(fOutputPath, StandardCharsets.UTF_8)) {
                while ((line = input.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            input.close();
            // --> reroute stdout to file
        } else {
            Process toolProcess = Runtime.getRuntime().exec(cmd);
            toolProcess.waitFor(360, TimeUnit.SECONDS);
        }
        return new SimpleEntry<>(pdfFile.toString(), fOutputPath.toString());
    }

}
