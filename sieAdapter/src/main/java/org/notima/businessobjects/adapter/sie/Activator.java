package org.notima.businessobjects.adapter.sie;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.generic.ifacebusinessobjects.AccountingFileExporter;
import org.notima.generic.ifacebusinessobjects.AccountingFileImporter;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(BusinessObjectFactory.class),
				@ProvideService(AccountingFileImporter.class),
				@ProvideService(AccountingFileExporter.class),
		}
)
public class Activator extends BaseActivator {
	
	private Logger log = LoggerFactory.getLogger(Activator.class);	
	
	@Override
	protected void doStop() {
		super.doStop();
		
	}

	@Override
	public void doStart() {
	
		Dictionary<String, String> props = new Hashtable<String,String>();
		props.put("SystemName", SieAdapter.SYSTEM_NAME);
		
		SieAdapter sapt = new SieAdapter();
		register(BusinessObjectFactory.class, sapt, props);
		log.info("SieAdapter registered");

		register(AccountingFileImporter.class, new SieAccountingFileImporter(), props);
		register(AccountingFileExporter.class, new SieAccountingFileExporter(), props);
		log.info("SIE accounting file importer / exporter registered");
		
	}


}
