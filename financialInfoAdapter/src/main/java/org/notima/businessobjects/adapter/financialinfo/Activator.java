package org.notima.businessobjects.adapter.financialinfo;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.generic.ifacebusinessobjects.FinancialInfoProvider;

/**
 * Registers the {@link FinancialInfoAdapter} as a {@link FinancialInfoProvider} service in Karaf.
 * Its settings are read from {@code financialinfo.properties}, see {@link FinancialInfoSettings}.
 */
@Services(provides = { @ProvideService(FinancialInfoProvider.class) })
public class Activator extends BaseActivator {

	@Override
	public void doStart() throws Exception {
		super.doStart();
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("SystemName", FinancialInfoAdapter.SYSTEM_NAME);
		register(FinancialInfoProvider.class, new FinancialInfoAdapter(), props);
	}

}
