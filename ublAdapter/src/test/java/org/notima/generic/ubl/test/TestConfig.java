package org.notima.generic.ubl.test;

import java.io.File;
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
	/**
	 * Local properties (not in git) overriding test.properties, ie with a real invoice and bankgiro.
	 * Choose another file with -Dubl.test.properties=my-test-other.properties
	 */
	private static final String LOCAL_PROPERTIES_FILE = System.getProperty("ubl.test.properties", "my-test.properties");

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
			// Try distinct file
			FileReader reader;
			File potentialFile = new File(srcFile1);
			if (!potentialFile.exists()) {
				URL url = ClassLoader.getSystemResource(srcFile1);
				if (url == null) {
					throw new Exception(srcFile1 + " not found. The src/test/resources folder needs to be in classpath when running tests.");
				}
				reader = new FileReader(url.getFile());
			} else {
				reader = new FileReader(potentialFile);
			}

			sampleBoInvoice = new Invoice<Object>();
			JAXBContext ctx = JAXBContext.newInstance(Invoice.class);

			Unmarshaller unmarshaller = ctx.createUnmarshaller();

			sampleBoInvoice = (Invoice<?>) unmarshaller.unmarshal(reader);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

}
