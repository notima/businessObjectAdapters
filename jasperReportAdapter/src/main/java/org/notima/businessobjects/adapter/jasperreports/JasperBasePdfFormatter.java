package org.notima.businessobjects.adapter.jasperreports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanArrayDataSource;

import org.notima.generic.ifacebusinessobjects.InvoiceFormatter;

public abstract class JasperBasePdfFormatter {

	public final static String[] formats = new String[] {
		"pdf"	
	};
	
	public final static String JASPER_FILE = "JasperFile";
	public final static String JASPER_LANG = "JasperLang";
	public final static String JASPER_REPORT_NAME = "JasperReportName";
	public final static String JASPER_OUTPUT_DIR = "JasperOutputDir";
	public final static String JASPER_OUTPUT_FILENAME = "JasperOutputFilename";

	/** Report directories extracted from jars, by jar URL + directory. */
	private static final Map<String, Path> extractedReportDirs = new HashMap<String, Path>();

	public String[] getFormats() {
		return formats;
	}

	/**
	 * Returns a bundled report resource as a file, so that it can be filled with
	 * {@link #formatReportAsPdf(Object[], String, JasperParameterCallback, Properties)}.
	 * <p>
	 * A resource inside a jar is extracted, together with the rest of its directory
	 * (subreports and images), to a temporary directory once per JVM. The bundled reports
	 * build paths as {@code SUBREPORT_DIR + "name"} as well as {@code SUBREPORT_DIR + "/name"},
	 * which only resolves on a file system.
	 *
	 * @param url		URL of the report resource.
	 * @return			The report file, or null if the URL is neither a file nor in a jar
	 * 					(e.g. an OSGi bundle URL).
	 */
	protected static synchronized File toReportFile(URL url) throws Exception {
		if ("file".equals(url.getProtocol())) {
			return new File(url.toURI());
		}
		if (!"jar".equals(url.getProtocol())) {
			return null;
		}
		JarURLConnection conn = (JarURLConnection) url.openConnection();
		String entryName = conn.getEntryName();
		String dirPrefix = entryName.substring(0, entryName.lastIndexOf('/') + 1);
		String key = conn.getJarFileURL() + "!/" + dirPrefix;

		Path dir = extractedReportDirs.get(key);
		if (dir == null) {
			dir = Files.createTempDirectory("jasper-reports");
			dir.toFile().deleteOnExit();
			JarFile jar = conn.getJarFile();
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry e = entries.nextElement();
				if (e.isDirectory() || !e.getName().startsWith(dirPrefix)
						|| e.getName().indexOf('/', dirPrefix.length()) >= 0) continue;
				Path target = dir.resolve(e.getName().substring(dirPrefix.length()));
				InputStream in = jar.getInputStream(e);
				try {
					Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
				} finally {
					in.close();
				}
				target.toFile().deleteOnExit();
			}
			extractedReportDirs.put(key, dir);
		}
		return dir.resolve(entryName.substring(dirPrefix.length())).toFile();
	}

	/**
	 * Output directory: {@link #JASPER_OUTPUT_DIR}, falling back to the generic
	 * {@link InvoiceFormatter#OUTPUT_DIR} so callers that only know the formatter
	 * interface can set it.
	 */
	protected String getOutputDir(Properties props) {
		return props.getProperty(JASPER_OUTPUT_DIR, props.getProperty(InvoiceFormatter.OUTPUT_DIR));
	}

	/**
	 * Output file name (without extension): {@link #JASPER_OUTPUT_FILENAME}, falling back
	 * to the generic {@link InvoiceFormatter#OUTPUT_FILENAME}.
	 */
	protected String getOutputFilename(Properties props) {
		return props.getProperty(JASPER_OUTPUT_FILENAME, props.getProperty(InvoiceFormatter.OUTPUT_FILENAME));
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
			outputDir = getOutputDir(props);
			jasperLang = props.getProperty(JASPER_LANG);
			jasperReportName = props.getProperty(JASPER_REPORT_NAME);
			jasperOutputFilename = getOutputFilename(props);
		}
		
		if (outputDir==null) {
			outputDir = System.getProperty("user.home");
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
			outputDir = getOutputDir(props);
			jasperLang = props.getProperty(JASPER_LANG);
			jasperReportName = props.getProperty(JASPER_REPORT_NAME);
			jasperOutputFilename = getOutputFilename(props);
		}

		if (outputDir == null) {
			outputDir = System.getProperty("user.home");
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
