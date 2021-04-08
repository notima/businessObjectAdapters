package org.notima.businessobjects.adapter.tools;

import java.io.File;
import java.util.Properties;

public abstract class BasicReportFormatter {

	public final static String OUTPUT_FILENAME = "OutputFilename";	
	public final static String OUTPUT_DIR = "OutputDir";
	
	protected String outputDir;
	protected String outputFileName;

	/**
	 * Sets default file properties (if any).
	 * 
	 * @param props		Properties
	 */
	public void setFromProperties(Properties props) {
		
		if (props==null) return;
		
		outputDir = props.getProperty(OUTPUT_DIR, System.getenv("user.home"));
		outputFileName = props.getProperty(OUTPUT_FILENAME, "outfile");
		
	}

	public String getOutputDir() {
		return outputDir;
	}

	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	public String getOutputFileName() {
		return outputFileName;
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}
	
	public String getPath() {
		if (outputDir!=null) {
			return outputDir + File.separator + outputFileName;
		} else {
			return outputFileName;
		}
	}
	
}
