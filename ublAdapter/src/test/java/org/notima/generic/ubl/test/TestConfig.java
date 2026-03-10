package org.notima.generic.ubl.test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.notima.generic.businessobjects.Invoice;

public class TestConfig {

	private static final String PROPERTIES_FILE = "test.properties";
	private static final String LOCAL_PROPERTIES_FILE = "my-test.properties";

	public static String srcFile1 = "my-test.xml";
	public static String resultDir = "target";
	public static String bankgiroNumber;
	public static String bankgiroAccountName;

	public static Invoice<?> sampleBoInvoice;

	public static void loadConfig() throws Exception {

		Properties props = new Properties();

		InputStream propsStream = ClassLoader.getSystemResourceAsStream(PROPERTIES_FILE);
		if (propsStream != null) {
			props.load(propsStream);
		}

		InputStream localPropsStream = ClassLoader.getSystemResourceAsStream(LOCAL_PROPERTIES_FILE);
		if (localPropsStream != null) {
			props.load(localPropsStream);
		}

		if (props.getProperty("sampleBoInvoiceFile") != null)
			srcFile1 = props.getProperty("sampleBoInvoiceFile");
		if (props.getProperty("resultDir") != null)
			resultDir = props.getProperty("resultDir");
		bankgiroNumber = props.getProperty("bankgiroNumber");
		bankgiroAccountName = props.getProperty("bankgiroAccountName");

		try {
			URL url = ClassLoader.getSystemResource(srcFile1);
			if (url == null) {
				throw new Exception(srcFile1 + " not found. The src/test/resources folder needs to be in classpath when running tests.");
			}
			FileReader reader = new FileReader(url.getFile());

			sampleBoInvoice = new Invoice<Object>();
			JAXBContext ctx = JAXBContext.newInstance(Invoice.class);

			Unmarshaller unmarshaller = ctx.createUnmarshaller();

			sampleBoInvoice = (Invoice<?>) unmarshaller.unmarshal(reader);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

}
