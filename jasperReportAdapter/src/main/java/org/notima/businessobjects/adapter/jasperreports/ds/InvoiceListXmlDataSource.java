package org.notima.businessobjects.adapter.jasperreports.ds;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXB;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceList;

public class InvoiceListXmlDataSource {

	public static final String INVOICELIST_XML_FILE = "INVOICELIST_XML_FILE";

	private static InvoiceList result;
	private static List<Invoice<?>> invoiceList = new ArrayList<Invoice<?>>();

	/** Creditor used by {@link #getCreditor()} when no invoice list file is configured. */
	private static final ThreadLocal<BusinessPartner<?>> currentCreditor = new ThreadLocal<>();
	
	/**
	 * Sets the creditor returned by {@link #getCreditor()} on this thread while no
	 * invoice list file is configured, e.g. the sender of the invoice being formatted.
	 * Pass {@code null} to clear it.
	 */
	public static void setCurrentCreditor(BusinessPartner<?> creditor) {
		if (creditor==null) {
			currentCreditor.remove();
		} else {
			currentCreditor.set(creditor);
		}
	}
	
	
	public static Collection<Invoice<?>> getInvoiceList() throws Exception {

		readFile();
		
		invoiceList.addAll(result.getInvoiceList());
			
		return invoiceList;
		
	}

	/**
	 * The creditor from the invoice list file named by {@link #INVOICELIST_XML_FILE}.
	 * When that isn't set, the creditor given by {@link #setCurrentCreditor(BusinessPartner)}.
	 */
	public static BusinessPartner<?> getCreditor() throws Exception {
		
		if (getXmlFileName()==null && currentCreditor.get()!=null) {
			return currentCreditor.get();
		}
		
		readFile();
		
		return result.getCreditor();
		
	}
	

	private static void readFile() throws Exception {

		String xmlFile = getXmlFileName();
		File inFile = null;
		if (xmlFile==null) {
			throw new Exception("Environment variable " + INVOICELIST_XML_FILE + " is not set.");
		} else {
			inFile = new File(xmlFile);
			if (!inFile.canRead()) {
				throw new Exception(xmlFile + " can't be read.");
			}
		}
		
		result = JAXB.unmarshal(inFile, InvoiceList.class);		
		
	}

	private static String getXmlFileName() {
		String xmlFile = System.getenv(INVOICELIST_XML_FILE);
		if (xmlFile==null) {
			xmlFile = System.getProperty(INVOICELIST_XML_FILE);
		}
		return xmlFile;
	}
	
	
	
	
}
