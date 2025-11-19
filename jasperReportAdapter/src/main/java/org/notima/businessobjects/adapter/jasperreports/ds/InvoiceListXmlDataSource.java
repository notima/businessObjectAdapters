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
	
	
	public static Collection<Invoice<?>> getInvoiceList() throws Exception {

		readFile();
		
		invoiceList.addAll(result.getInvoiceList());
			
		return invoiceList;
		
	}

	public static BusinessPartner<?> getCreditor() throws Exception {
		
		readFile();
		
		return result.getCreditor();
		
	}
	

	private static void readFile() throws Exception {

		String xmlFile = System.getenv(INVOICELIST_XML_FILE);
		if (xmlFile==null) {
			xmlFile = System.getProperty(INVOICELIST_XML_FILE);
		}
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
	
	
	
	
}
