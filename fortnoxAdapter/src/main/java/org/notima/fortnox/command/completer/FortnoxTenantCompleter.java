package org.notima.fortnox.command.completer;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.businessobjects.adapter.tools.FactorySelector;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;


@Service
public class FortnoxTenantCompleter implements Completer {
	
	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;

	private List<String> tenantOrgNos;
	
	@Override
	public int complete(Session session, CommandLine commandLine, List<String> candidates) {

		buildList();
		
		StringsCompleter delegate = new StringsCompleter();
		for (int i = 0; i<tenantOrgNos.size(); i++) {
			delegate.getStrings().add(tenantOrgNos.get(i));
		}
		return delegate.complete(session, commandLine, candidates);
		
	}
	
	private void buildList() {
		
		tenantOrgNos = new ArrayList<String>();
		FactorySelector selector = new FactorySelector(bofs);
		BusinessObjectFactory<?, ?, ?, ?, ?, ?> bf = selector.getFirstFactoryFor(FortnoxAdapter.SYSTEMNAME);
		BusinessPartnerList<?> bpl = bf.listTenants();
		
		if (bpl!=null && bpl.getBusinessPartner()!=null && bpl.getBusinessPartner().size()>0) {
			for (BusinessPartner<?> bp : bpl.getBusinessPartner()) {
				if (bp.getTaxId()!=null)
					tenantOrgNos.add(bp.getTaxId());
			}
		}
		
	}
	
	
}
