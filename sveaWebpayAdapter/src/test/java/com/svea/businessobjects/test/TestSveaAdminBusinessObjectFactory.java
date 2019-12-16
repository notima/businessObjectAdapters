package com.svea.businessobjects.test;

import static org.junit.Assert.fail;

import java.io.FileReader;
import java.net.URL;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;

import com.svea.businessobjects.sveaadmin.SveaAdminBusinessObjectFactory;
import com.svea.webpay.common.auth.ListOfSveaCredentials;
import com.svea.webpay.common.auth.SveaCredential;

public class TestSveaAdminBusinessObjectFactory {

	protected Logger	log = org.slf4j.LoggerFactory.getLogger(this.getClass());
	
	protected ListOfSveaCredentials	credentials = new ListOfSveaCredentials();	
	protected String TEST_CONFIG = "config-test.xml";
	protected String TEST_PROPERTY_FILE = "test-details.properties";
	
	protected SveaAdminBusinessObjectFactory bof;
	
	protected Properties testProperties = new Properties();
	
	
	@Before
	public void setUp() throws Exception {
		
		URL url = ClassLoader.getSystemResource(TEST_CONFIG);
		
		if (url==null) {
			fail("The file " + TEST_CONFIG + "  must exist in classpath for unit tests to work.\n" +
				 "Copy the file config-template.xml in src/test/resources to config-test.xml and fill in login details.");
		}

		credentials.setCredentials(SveaCredential.loadCredentialsFromXmlFile(TEST_CONFIG));
		
		
		for (SveaCredential cre : credentials.getCredentials()) {
			
			if (cre.getAccountNo()!=null && cre.getAccountNo().trim().length()>0) {
			
				bof = new SveaAdminBusinessObjectFactory();
				bof.initCredentials(cre);
				
			} 
			
		}
		
		// Find test properties file
		url = ClassLoader.getSystemResource(TEST_PROPERTY_FILE);
		if (url==null) {
			fail("The property file " + TEST_PROPERTY_FILE + " must exist in classpath for unit tests to work.\n" +
				 "Copy the file test-details-template.properties in src/test/resources to " + TEST_PROPERTY_FILE + " and fill in order details."
				);
		}
		testProperties.load(new FileReader(url.getFile()));
		
	}

	@After
	public void tearDown() throws Exception {
	}
	

}
