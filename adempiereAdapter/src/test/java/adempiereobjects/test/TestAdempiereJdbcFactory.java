package adempiereobjects.test;

import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Properties;

import javax.xml.bind.JAXB;

import org.junit.BeforeClass;
import org.junit.Test;
import org.notima.generic.adempiere.factory.AdempiereJdbcFactory;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

public class TestAdempiereJdbcFactory {

	private static Properties props;
	private static String		url;
	private static String		user;
	private static String		password;
	private static int			clientId;
	private static int			orgId;
	
	private static BusinessObjectFactory	factory;
	
	@BeforeClass
	public static void setUp() throws Exception {
		// Read connection properties
		InputStream is = ClassLoader.getSystemResourceAsStream("connection.properties");
		if (is==null) return;
		props = new Properties();
		props.load(is);
		
		url = props.getProperty("url");
		user = props.getProperty("user");
		password = props.getProperty("password");
		clientId = 1000000;
		orgId = 1000000;

		try {
			factory = new AdempiereJdbcFactory(url, user, password, clientId, orgId);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}

	// @Test
	public void testLookupProductByEan() {

		if (factory==null) return;
		try {
			//Product product = factory.lookupProductByEan("074603003287");
			//Invoice invoice = factory.lookupInvoice("1000887");
			DunningRun dun = factory.lookupDunningRun("1000002", null);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}

	@Test
	public void testLookupInvoice() {
		if (factory==null) return;
		
		try {
			Invoice invoice = factory.lookupInvoice("8774517");
			JAXB.marshal(invoice, System.out);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	// @Test
	public void testLookupCreditInvoice() {
		if (factory==null) return;
		
		try {
			Invoice invoice = factory.lookupInvoice("211120");
			JAXB.marshal(invoice, System.out);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	

}
