package org.notima.businessobjects.adapter.tools;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Properties;

public abstract class BasicReportFormatter {

	public final static String OUTPUT_FILENAME = "OutputFilename";	
	public final static String OUTPUT_DIR = "OutputDir";
	public final static String DATE_FORMAT = "DateFormat";
	
	protected String outputDir;
	protected String outputFileName;
	protected String dateFormatStr;
	

	protected SimpleDateFormat	dfmt;
	protected Locale locale;
	
	/**
	 * Sets default file properties (if any).
	 * 
	 * @param props		Properties
	 */
	public void setFromProperties(Properties props) {
		
		locale = Locale.getDefault();
		dfmt =  (SimpleDateFormat) SimpleDateFormat.getDateInstance(
                SimpleDateFormat.SHORT, locale);
		String dateFormatDefaultPattern = dfmt.toPattern();
		
		if (props==null) return;
		
		outputDir = props.getProperty(OUTPUT_DIR, System.getenv("user.home"));
		outputFileName = props.getProperty(OUTPUT_FILENAME, "outfile");
		dateFormatStr = props.getProperty(DATE_FORMAT, dateFormatDefaultPattern);

		dfmt = new SimpleDateFormat(dateFormatStr, locale);
		
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
