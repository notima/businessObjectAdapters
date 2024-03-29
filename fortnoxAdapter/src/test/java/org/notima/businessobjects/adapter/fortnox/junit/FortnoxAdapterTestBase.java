package org.notima.businessobjects.adapter.fortnox.junit;

import java.net.URL;
import java.text.SimpleDateFormat;

import org.junit.Before;
import org.notima.api.fortnox.FortnoxUtil;
import org.notima.api.fortnox.clients.FortnoxClientInfo;
import org.notima.api.fortnox.clients.FortnoxClientList;
import org.notima.api.fortnox.clients.FortnoxClientManager;
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
		
		URL configUrl = ClassLoader.getSystemResource("fortnoxClients.xml");
		
		if (configUrl==null) {
			throw new Exception("No fortnoxClients.xml found");
		}

		FortnoxClientList clientList = FortnoxUtil.readFortnoxClientListFromFile(configUrl.getFile());
		
		FortnoxClientInfo info = clientList.getFirstClient();
		
		String orgNo = info.getOrgNo();
		
		FortnoxClientManager mgr = new FortnoxClientManager(configUrl.getFile());
		
		factory = new FortnoxAdapter(orgNo);
		
	}
	
}
