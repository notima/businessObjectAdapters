package org.notima.businessobjects.adapter.jasperreports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanArrayDataSource;

public abstract class JasperBasePdfFormatter {

	public final static String[] formats = new String[] {
		"pdf"	
	};
	
	public final static String JASPER_FILE = "JasperFile";
	public final static String JASPER_LANG = "JasperLang";
	public final static String JASPER_REPORT_NAME = "JasperReportName";
	public final static String JASPER_OUTPUT_DIR = "JasperOutputDir";
	public final static String JASPER_OUTPUT_FILENAME = "JasperOutputFilename";

	public String[] getFormats() {
		return formats;
	}

	/**
	 * Adds additional parameters from a properties object
	 * 
	 * @param parameters	The parameters that are to be added to.
	 * @param props			The properties that are to be added.
	 */
	public void addAdditionalJasperParameters(HashMap<String, Object> parameters, Properties props) {
		
		Enumeration<Object> keys = props.keys();
		
		Object key;
		while (keys.hasMoreElements()) {
			key = keys.nextElement();
			parameters.put(key.toString(), props.get(key));
		}
		
	}
	
	/**
	 * Formats a report as a PDF-file
	 * 
	 * @param data				The data that should be sent to the report.
	 * @param jasperFile		The jasper file to create the report from.
	 * @param jpc				A callback with additional properties. Useful when overriding this class.
	 * @param props				Properties for this call.
	 * @return					The absolut file name of the PDF-created.
	 * @throws Exception		Exception if something goes wrong.
	 */
	public String formatReportAsPdf(Object[] data, String jasperFile, JasperParameterCallback jpc, Properties props) throws Exception {

		if (data==null) {
			throw new Exception("Data can't be null");
		}
		
		String outputDir = null;
		String jasperLang = null;
		String jasperReportName = null;
		String jasperOutputFilename = null;
		
		if (props!=null) {
			if (jasperFile==null)
				jasperFile = props.getProperty(JASPER_FILE);
			outputDir = props.getProperty(JASPER_OUTPUT_DIR);
			jasperLang = props.getProperty(JASPER_LANG);
			jasperReportName = props.getProperty(JASPER_REPORT_NAME);
			jasperOutputFilename = props.getProperty(JASPER_OUTPUT_FILENAME);
		}
		
		if (outputDir==null) {
			outputDir = System.getenv("user.home");
		}
		if (jasperOutputFilename==null) {
			jasperOutputFilename = "JasperReport";
		}
		
		if (jasperFile!=null) {
			File f = new File(jasperFile);
			if (!f.canRead()) {
				throw new Exception(jasperFile + " can't be read.");
			}
		} else {
			// Lookup default jasper file as a resource
			URL url = ClassLoader.getSystemResource("reports/OrderList.jasper");
			if (url!=null) {
				jasperFile = url.getFile();
			} else {
				throw new Exception("The property " + JASPER_FILE + " must be set.");
			}
		}
		
    	// set parameters
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("SUBREPORT_DIR", jasperFile.substring(0, jasperFile.lastIndexOf(File.separator) + 1));
		parameters.put(JASPER_REPORT_NAME, jasperReportName);
		if (jasperLang!=null) {
			parameters.put("CURRENT_LANG", jasperLang);
		}
		if (jpc!=null && jpc.getExtraProperties()!=null) {
			addAdditionalJasperParameters(parameters, jpc.getExtraProperties());
		}

		InputStream is = new FileInputStream(jasperFile);
		
		JasperPrint print = JasperFillManager.fillReport(is, parameters, new JRBeanArrayDataSource(data));
		
		File resultPdf = createPdfFile(print, outputDir, null, jasperOutputFilename);
		
		return resultPdf.getAbsolutePath();
	}

	/**
	 * Formats a report as a PDF-file using a URL to the jasper resource.
	 * Use this in OSGI environments where classloader resources live inside a bundle
	 * and cannot be resolved to a plain filesystem path via url.getFile().
	 *
	 * @param data				The data that should be sent to the report.
	 * @param jasperUrl			URL of the jasper resource (e.g. from getClass().getClassLoader().getResource(...))
	 * @param jpc				A callback with additional properties.
	 * @param props				Properties for this call.
	 * @return					The absolute file name of the PDF created.
	 * @throws Exception		Exception if something goes wrong.
	 */
	public String formatReportAsPdf(Object[] data, URL jasperUrl, JasperParameterCallback jpc, Properties props) throws Exception {

		if (data == null) {
			throw new Exception("Data can't be null");
		}
		if (jasperUrl == null) {
			throw new Exception("jasperUrl can't be null");
		}

		String outputDir = null;
		String jasperLang = null;
		String jasperReportName = null;
		String jasperOutputFilename = null;

		if (props != null) {
			outputDir = props.getProperty(JASPER_OUTPUT_DIR);
			jasperLang = props.getProperty(JASPER_LANG);
			jasperReportName = props.getProperty(JASPER_REPORT_NAME);
			jasperOutputFilename = props.getProperty(JASPER_OUTPUT_FILENAME);
		}

		if (outputDir == null) {
			outputDir = System.getenv("user.home");
		}
		if (jasperOutputFilename == null) {
			jasperOutputFilename = "JasperReport";
		}

		// Derive SUBREPORT_DIR from the URL so subreports in the same directory resolve correctly
		String urlStr = jasperUrl.toString();
		String subreportDir = urlStr.substring(0, urlStr.lastIndexOf('/') + 1);

		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("SUBREPORT_DIR", subreportDir);
		parameters.put(JASPER_REPORT_NAME, jasperReportName);
		if (jasperLang != null) {
			parameters.put("CURRENT_LANG", jasperLang);
		}
		if (jpc != null && jpc.getExtraProperties() != null) {
			addAdditionalJasperParameters(parameters, jpc.getExtraProperties());
		}

		InputStream is = jasperUrl.openStream();
		JasperPrint print = JasperFillManager.fillReport(is, parameters, new JRBeanArrayDataSource(data));
		File resultPdf = createPdfFile(print, outputDir, null, jasperOutputFilename);
		return resultPdf.getAbsolutePath();
	}

	/**
	 * Creates a file from the PDF and saves it
	 * 
	 * @param print						The print
	 * @param exportPath				The export path to use
	 * @param exportFolder				Export folder appended after the path
	 * @param exportFileName			The export file name
	 * @throws FileNotFoundException	If the export path can't be found.
	 * @throws JRException				If there's a jasper report exception			
	 */
	protected File createPdfFile(JasperPrint print, String exportPath, String exportFolder, String exportFileName) throws FileNotFoundException, JRException{
		String filePath;
		
		filePath = (exportPath!=null ? exportPath + File.separator : "." + File.separator) + (exportFolder!=null ? exportFolder + File.separator : "") + exportFileName + ".pdf";
		//Writes the file to disk
		File pdf = new File(filePath);
    	pdf.getParentFile().mkdirs();
    	JasperExportManager.exportReportToPdfStream(print, new FileOutputStream(pdf));
    	return pdf;
    	
	}
	
}
