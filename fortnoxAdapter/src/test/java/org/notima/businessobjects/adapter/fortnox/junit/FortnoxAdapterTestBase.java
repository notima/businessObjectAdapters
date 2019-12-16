package org.notima.businessobjects.adapter.fortnox.junit;

import java.net.URL;
import java.text.SimpleDateFormat;

import org.apache.commons.configuration.CompositeConfiguration;
import org.junit.Before;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.slf4j.Logger;

public class FortnoxAdapterTestBase {

	protected Logger	log = org.slf4j.LoggerFactory.getLogger(this.getClass());
	
	@SuppressWarnings("rawtypes")
	protected BusinessObjectFactory factory;
	protected SimpleDateFormat	  dfmt = new SimpleDateFormat("yyyy-MM-dd");
	
	@Before
	public void setUp() throws Exception {
		
		CompositeConfiguration config = new CompositeConfiguration();
		URL configUrl = ClassLoader.getSystemResource("test-config3.xml");
		config.addConfiguration(new org.apache.commons.configuration.XMLConfiguration(configUrl));
		
		String clientSecret = config.getString("clientSecret");
		String accessToken = config.getString("accessToken");
		
		if (clientSecret==null || clientSecret.trim().length()==0) {
			throw new Exception("Client secret must be supplied.");
		}

		if (accessToken==null || accessToken.trim().length()==0) {
			throw new Exception("Access token must be supplied.");
		}
		
		
		factory = new FortnoxAdapter(accessToken, clientSecret);
		
	}
	
}
