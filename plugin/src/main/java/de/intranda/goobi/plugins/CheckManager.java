package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.goobi.beans.Process;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.StorageProviderInterface;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.App;
import edu.harvard.hul.ois.jhove.JhoveBase;
import edu.harvard.hul.ois.jhove.JhoveException;
import edu.harvard.hul.ois.jhove.OutputHandler;

public class CheckManager {
	
	private HashMap<String, ToolConfiguration> toolConfigurations;
	private List<List<Check>> ingestLevels;

	private Path outputPath;
	private List<Path> pdfsInFolder = new ArrayList<>();
	// mayber refactor later

	public CheckManager(HashMap<String, ToolConfiguration> toolsConfigurations, List<List<Check>> ingestLevels,
			Process process) throws IOException, InterruptedException, SwapException, DAOException {
		// readConfiguration

		// TODO implement FilenameFilter here!
		//this.pdfsInFolder.addAll(StorageProvider.getInstance().listFiles(process.getSourceDirectory()));
		this.pdfsInFolder.addAll(StorageProvider.getInstance().listFiles("/opt/digiverso/pdf"));
		//TODO fix this
		String test = process.getProcessDataDirectory();
		this.outputPath = Paths.get(test, "validation",
				System.currentTimeMillis() + "_xls");
	}

	public CheckManager(HashMap<String, ToolConfiguration> toolsConfigurations, List<List<Check>> ingestLevels) {
		this.toolConfigurations = toolsConfigurations;
		this.ingestLevels = ingestLevels;
		// pdfsfolder
	}

	private HashMap<String, List<SimpleEntry<String, String>>> runTools() throws IOException, InterruptedException {
		HashMap<String, List<SimpleEntry<String, String>>> resultFiles = new HashMap();
		for (String toolName : this.toolConfigurations.keySet()) {
			List<SimpleEntry<String, String>> results = new ArrayList();
			for (Path pdfFile : this.pdfsInFolder) {
				ToolConfiguration tc = this.toolConfigurations.get(toolName);
				ToolRunner tr = new ToolRunner(tc, outputPath);
				results.add(tr.runTool(pdfFile));
			}
			resultFiles.put(toolName, results);
		}
		return resultFiles;
	}

	public boolean runChecks(int targetLevel) throws IOException, InterruptedException {
		HashMap<String, List<SimpleEntry<String, String>>> results = runTools();
		
//		SAXBuilder jdomBuilder = new SAXBuilder();
//		Document jdomDocument;
//		boolean error = false;
//
//		for (String toolName : results.keySet()) {
//			List<SimpleEntry<String, String>> resultFiles = results.get(toolName);

//			for (SimpleEntry<String, String> resultFile : resultFiles) {
//				try {
//					jdomDocument = jdomBuilder.build(resultFile.getValue());
//					for (int level = 0; level < ingestLevels.size(); level++) {
//						List<Check> checks = ingestLevels.get(level);
//						checks.stream().filter(check -> check.getTool().equals(toolName)).forEach(check -> {
//							if  !check  (jdomDocument) {
//								error = true;
								//add negative Status
								
//								
//							}
//						});
//					}
//
//				} catch (JDOMException | IOException | IllegalStateException e) {
//					//TODO  handleException(e);
//					
//				}
//			}
//		}
		
		return false;
	}

	private boolean runcheck(Check check, Path file) {

		// TODO Auto-generated method stub
		return false;
	}

}
