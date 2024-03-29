package adempiereobjects.test;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.notima.generic.adempiere.factory.AdempiereJdbcFactory;
import org.notima.generic.adempiere.factory.IDempiereAptMapper;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;

public class TestAptMapper {
	
	private AdempiereJdbcFactory factory;
	
	@Before
	public void setUp() throws Exception {
		TestAdempiereJdbcFactory.setUp();
		factory = (AdempiereJdbcFactory)TestAdempiereJdbcFactory.factory;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAptMapper() throws Exception {

		boolean connected = factory.isConnected();
		assertTrue("Database is connected", connected);
		
		IDempiereAptMapper aptMapper = new IDempiereAptMapper(factory.getDataSource(), factory.getADOrgId());
		TaxSubjectIdentifier customer = aptMapper.mapApartmentNoToTaxSubject("A01");
		System.out.println(customer.getTaxId());
		
	}

}
