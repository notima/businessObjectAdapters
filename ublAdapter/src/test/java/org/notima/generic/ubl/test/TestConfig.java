package org.notima.generic.ubl.test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;

import javax.xml.bind.JAXB;

import org.notima.generic.businessobjects.Invoice;

public class TestConfig {

	public static String srcFile1 = "sample-bo-invoice.xml";
	
	public static Invoice sampleBoInvoice;
	
	public static void loadConfig() {

		try {
			URL url = ClassLoader.getSystemResource(srcFile1);
			FileReader reader = new FileReader(url.getFile());
			
			sampleBoInvoice = (Invoice)JAXB.unmarshal(reader, Invoice.class);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
