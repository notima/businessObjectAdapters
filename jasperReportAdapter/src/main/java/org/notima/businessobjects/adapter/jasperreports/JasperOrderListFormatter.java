package org.notima.businessobjects.adapter.jasperreports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

import org.notima.businessobjects.adapter.tools.OrderListFormatter;
import org.notima.generic.businessobjects.OrderList;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanArrayDataSource;

public class JasperOrderListFormatter implements OrderListFormatter {

	public final static String[] formats = new String[] {
		"pdf"	
	};
	
	public final static String JASPER_FILE = "JasperFile";
	public final static String JASPER_COMPANY_NAME = "JasperCompanyName";
	public final static String JASPER_TAX_ID = "JasperTaxId";
	public final static String JASPER_LANG = "JasperLang";
	public final static String JASPER_REPORT_NAME = "JasperReportName";
	public final static String OUTPUT_DIR = "OutputDir";

	@Override
	public String[] getFormats() {
		return formats;
	}
	
	@Override
	public String formatOrderList(OrderList orderList, String format, Properties props) throws Exception {

		if (orderList==null || orderList.getOrderList()==null) {
			throw new Exception("OrderList can't be null");
		}
		
		String outputDir = null;
		String jasperFile = null;
		String jasperCompanyName = null;
		String jasperTaxId = null;
		String jasperLang = null;
		String jasperReportName = null;
		
		if (props!=null) {
			jasperFile = props.getProperty(JASPER_FILE);
			outputDir = props.getProperty(OUTPUT_DIR);
			jasperCompanyName = props.getProperty(JASPER_COMPANY_NAME);
			jasperTaxId = props.getProperty(JASPER_TAX_ID);
			jasperLang = props.getProperty(JASPER_LANG);
			jasperReportName = props.getProperty(JASPER_REPORT_NAME);
			if (outputDir==null) {
				outputDir = System.getenv("user.home");
			}
		}
		if (jasperFile!=null) {
			File f = new File(jasperFile);
			if (!f.canRead()) {
				throw new Exception(jasperFile + " can't be read.");
			}
		} else {
			// Lookup default jasper file as a resource
			URL url = ClassLoader.getSystemResource("OrderList.jasper");
			if (url!=null) {
				jasperFile = url.getFile();
			} else {
				throw new Exception("The property " + JASPER_FILE + " must be set.");
			}
		}
		
    	// set parameters
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("SUBREPORT_DIR", jasperFile.substring(0, jasperFile.lastIndexOf("/") + 1));
		parameters.put(JASPER_COMPANY_NAME, jasperCompanyName);
		parameters.put(JASPER_TAX_ID, jasperTaxId);
		parameters.put(JASPER_REPORT_NAME, jasperReportName);
		if (jasperLang!=null) {
			parameters.put("CURRENT_LANG", jasperLang);
		}

		InputStream is = new FileInputStream(jasperFile);
		
		JasperPrint print = JasperFillManager.fillReport(is, parameters, new JRBeanArrayDataSource(orderList.getOrderList().toArray()));
		
		File resultPdf = createPdfFile(print, outputDir, null, "OrderList");
		
		return resultPdf.getAbsolutePath();
	}

	/**
	 * Creates a file from the PDF and saves it
	 * 
	 * @param print
	 * @param dr
	 * @param exportPath
	 * @param exportFolder
	 * @param exportFileName
	 * @throws FileNotFoundException
	 * @throws JRException
	 */
	private File createPdfFile(JasperPrint print, String exportPath, String exportFolder, String exportFileName) throws FileNotFoundException, JRException{
		String filePath;
		
		filePath = (exportPath!=null ? exportPath + File.separator : "." + File.separator) + (exportFolder!=null ? exportFolder + File.separator : "") + exportFileName + ".pdf";
		//Writes the file to disk
		File pdf = new File(filePath);
    	pdf.getParentFile().mkdirs();
    	System.out.println(pdf.getAbsolutePath());
    	JasperExportManager.exportReportToPdfStream(print, new FileOutputStream(pdf));
    	return pdf;
    	
	}
	
}
