package org.notima.businessobjects.adapter.infometric.junit;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.notima.businessobjects.adapter.infometric.InfometricAdapter;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.OrderInvoice;
import org.slf4j.Logger;

public class TestInfometricAdapter {

	protected Logger	log = org.slf4j.LoggerFactory.getLogger(this.getClass());
	
	private URL csvTestFile;
	
	@SuppressWarnings("rawtypes")
	protected BusinessObjectFactory factory;
	protected SimpleDateFormat	  dfmt = new SimpleDateFormat("yyyy-MM-dd");
	
	@Before
	public void setUp() throws Exception {
		
		csvTestFile = ClassLoader.getSystemResource("Billing_200101.txt");
		
		if (csvTestFile==null) {
			throw new Exception("Billing file found");
		}
		
		
	}
	
	@Test
	public void testSplitBillingFile() throws Exception {

		String content = new String ( Files.readAllBytes( Paths.get(csvTestFile.toURI())));
		
		InfometricAdapter adapter = new InfometricAdapter();
		List<OrderInvoice> result = adapter.splitBillingFile("105", 1.50, "Testresource", content);
		log.info("{} invoices created.", result.size());
		
	}
	
	
}
