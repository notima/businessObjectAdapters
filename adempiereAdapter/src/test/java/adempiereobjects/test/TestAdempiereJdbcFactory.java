package adempiereobjects.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.notima.generic.adempiere.factory.AdempiereJdbcFactory;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

public class TestAdempiereJdbcFactory {

	private static Properties props;
	private static String		url;
	private static String		user;
	private static String		password;
	private static int			clientId;
	private static int			orgId;
	
	public static BusinessObjectFactory<?, ?, ?, ?, ?, ?>	factory;
	
	@BeforeClass
	public static void setUp() throws Exception {
		// Read connection properties
		InputStream is = ClassLoader.getSystemResourceAsStream("connection.properties");
		if (is==null) return;
		props = new Properties();
		props.load(is);
		
		url = props.getProperty("adempiere.db.url");
		user = props.getProperty("adempiere.db.user");
		password = props.getProperty("adempiere.db.password");
		clientId = 1000000;
		orgId = 1000000;

		try {
			factory = new AdempiereJdbcFactory(url, user, password, clientId, orgId);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}

	@Test
	public void testIsConnected() {

		if (factory==null) fail("No connection created");
		try {
			boolean connected = factory.isConnected();
			assertTrue("Database is connected", connected);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	

}
