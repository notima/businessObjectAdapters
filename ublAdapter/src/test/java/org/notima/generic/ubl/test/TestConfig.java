package org.notima.generic.ubl.test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.notima.generic.businessobjects.Invoice;

public class TestConfig {

	public static String srcFile1 = "my-test.xml";
	
	public static Invoice<?> sampleBoInvoice;
	
	public static void loadConfig() throws Exception {

		try {
			URL url = ClassLoader.getSystemResource(srcFile1);
			if (url==null) {
				throw new Exception(srcFile1 + " not found. The src/test/resources folder needs to be in classpath when running tests.");
			}
			FileReader reader = new FileReader(url.getFile());
			
			sampleBoInvoice = new Invoice<Object>();
			JAXBContext ctx = JAXBContext.newInstance(Invoice.class);
			
			Unmarshaller unmarshaller = ctx.createUnmarshaller();
			
			sampleBoInvoice = (Invoice<?>)unmarshaller.unmarshal(reader);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
