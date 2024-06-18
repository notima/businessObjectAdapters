package org.notima.businessobjects.adapter.infometric.junit;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.notima.businessobjects.adapter.infometric.BillingFileToInvoiceList;
import org.notima.businessobjects.adapter.infometric.InfometricAdapter;
import org.notima.businessobjects.adapter.infometric.InfometricTenant;
import org.notima.generic.businessobjects.InvoiceList;
import org.notima.generic.businessobjects.InvoiceOperationResult;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.slf4j.Logger;

public class TestInfometricAdapter {

	protected Logger	log = org.slf4j.LoggerFactory.getLogger(this.getClass());
	
	private URL csvTestFile;
	
	private InfometricAdapter adapter;
	private InfometricTenant tenant;
	
	@SuppressWarnings("rawtypes")
	protected BusinessObjectFactory factory;
	protected SimpleDateFormat	  dfmt = new SimpleDateFormat("yyyy-MM-dd");
	
	@Before
	public void setUp() throws Exception {
		
		csvTestFile = ClassLoader.getSystemResource("Billing_200101.txt");
		
		if (csvTestFile==null) {
			throw new Exception("Billing file found");
		}
		
		adapter = new InfometricAdapter();
		adapter.setInfometricBaseDirectory(System.getProperty("user.dir"));
		
		Properties props = new Properties();
		props.setProperty(InfometricAdapter.PROP_TENANTDIRECTORY, "src/test/resources");
		props.setProperty(InfometricAdapter.PROP_BILLINGPRODUCT, "100");
		
		adapter.addTenant("555555-5555", "SE", "Test AB", props);
		
	}
	
	@Test
	public void testSplitBillingFile() throws Exception {
		
		String content = new String ( Files.readAllBytes( Paths.get(csvTestFile.toURI())));
		
		BillingFileToInvoiceList filesToInvoice = new BillingFileToInvoiceList(adapter, tenant);
		InvoiceList result = filesToInvoice.splitBillingFile(tenant.getTenantSettings(), 1.50, content);
		
		log.info("{} invoices created.", result.getInvoiceList().size());
		
	}
	
	
	@Test
	public void testReadAllFiles() throws Exception {

		InvoiceOperationResult result = adapter.readInvoices(null, null, 1);
		
		log.info("{} invoices created.", result.getAffectedInvoices().getInvoiceList().size());
		
	}
	
	
	
}
